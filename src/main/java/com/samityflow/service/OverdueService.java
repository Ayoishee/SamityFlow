package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.pattern.observer.LoanObserver;
import com.samityflow.pattern.strategy.CalculationStrategyFactory;
import com.samityflow.repository.AuditLogRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class OverdueService {
    private final Database database;
    private final List<LoanObserver> observers = new ArrayList<>();

    public OverdueService(Database database) {
        this.database = database;
    }

    public void addObserver(LoanObserver observer) {
        if (observer != null) observers.add(observer);
    }

    public int scan(LocalDate asOfDate) {
        if (asOfDate == null) throw new IllegalArgumentException("Scan date is required");
        List<String> alerts = new ArrayList<>();

        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                String sql = """
                        SELECT i.installment_id, i.due_date, i.total_amount, i.paid_amount,
                               i.penalty_amount,
                               l.loan_id, l.member_id, lp.penalty_strategy, lp.penalty_rate
                        FROM installments i
                        JOIN loans l ON l.loan_id=i.loan_id
                        JOIN loan_applications a ON a.id=l.application_id
                        JOIN loan_products lp ON lp.id=a.product_id
                        WHERE i.due_date < ?
                          AND i.status IN ('PENDING','PARTIALLY_PAID','OVERDUE')
                        """;

                int count = 0;
                try (PreparedStatement query = connection.prepareStatement(sql)) {
                    query.setString(1, asOfDate.toString());
                    try (ResultSet rs = query.executeQuery()) {
                        while (rs.next()) {
                            int installmentId = rs.getInt("installment_id");
                            LocalDate dueDate = LocalDate.parse(rs.getString("due_date"));
                            int overdueWeeks = Math.max(1,
                                    (int) (ChronoUnit.DAYS.between(dueDate, asOfDate) / 7) + 1);
                            BigDecimal overdueAmount = rs.getBigDecimal("total_amount")
                                    .subtract(rs.getBigDecimal("paid_amount"));
                            double penaltyValue = CalculationStrategyFactory
                                    .penalty(rs.getString("penalty_strategy"))
                                    .calculate(overdueAmount.doubleValue(),
                                            rs.getDouble("penalty_rate"), overdueWeeks);
                            BigDecimal penalty = BigDecimal.valueOf(penaltyValue)
                                    .setScale(2, RoundingMode.HALF_UP);

                            updateInstallment(connection, installmentId, penalty);
                            addPenaltyDifferenceToLoan(connection, rs.getInt("loan_id"),
                                    penalty.subtract(rs.getBigDecimal("penalty_amount")));
                            if (overdueWeeks >= 3) {
                                restrictMemberAndLoan(connection,
                                        rs.getInt("member_id"), rs.getInt("loan_id"));
                            }
                            new AuditLogRepository(connection).save(
                                    "INSTALLMENT_OVERDUE", "INSTALLMENT", installmentId,
                                    "SYSTEM", "Overdue weeks=" + overdueWeeks + "; penalty=" + penalty);
                            alerts.add("Installment " + installmentId + " is overdue");
                            count++;
                        }
                    }
                }
                connection.commit();
                alerts.forEach(this::notifyObservers);
                return count;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Overdue scan failed", exception);
        }
    }

    private void updateInstallment(Connection connection, int id, BigDecimal penalty)
            throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE installments SET penalty_amount=?, status='OVERDUE' WHERE installment_id=?")) {
            ps.setBigDecimal(1, penalty);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private void restrictMemberAndLoan(Connection connection, int memberId, int loanId)
            throws Exception {
        try (PreparedStatement member = connection.prepareStatement(
                "UPDATE members SET eligible=0 WHERE id=?");
             PreparedStatement loan = connection.prepareStatement(
                     "UPDATE loans SET status='DEFAULTED' WHERE loan_id=?")) {
            member.setInt(1, memberId);
            member.executeUpdate();
            loan.setInt(1, loanId);
            loan.executeUpdate();
        }
    }

    private void addPenaltyDifferenceToLoan(Connection connection, int loanId,
                                            BigDecimal difference) throws Exception {
        if (difference.compareTo(BigDecimal.ZERO) == 0) return;
        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE loans SET outstanding_balance=outstanding_balance+? WHERE loan_id=?
                """)) {
            ps.setBigDecimal(1, difference);
            ps.setInt(2, loanId);
            ps.executeUpdate();
        }
    }

    private void notifyObservers(String event) {
        observers.forEach(observer -> observer.update(event));
    }
}
