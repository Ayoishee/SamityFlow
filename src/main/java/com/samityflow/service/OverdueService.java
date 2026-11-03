package com.samityflow.service;

import com.samityflow.database.Database;
import com.samityflow.pattern.observer.LoanObserver;
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

/** Accrues unassessed penalty weeks and marks prolonged overdue loans for review. */
public class OverdueService {
    private final Database database;
    private final List<LoanObserver> observers = new ArrayList<>();

    public OverdueService(Database database) {
        this.database = database;
    }

    /** Registers a non-null alert listener; notifications occur after the scan commits. */
    public void addObserver(LoanObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    /**
     * Accrues unassessed penalty weeks on remaining base debt at the scan date and returns overdue
     * installment count. Repeated dates add no charge. Missing legacy terms or unreconciled
     * penalties throw IllegalStateException; database mutations are atomic on an owned connection.
     */
    public int scan(LocalDate asOfDate) {
        if (asOfDate == null) {
            throw new IllegalArgumentException("Scan date is required");
        }
        List<String> alerts = new ArrayList<>();

        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (var check = connection.createStatement();
                        var missing =
                                check.executeQuery(
                                        "SELECT COUNT(*) FROM installments i JOIN loans l ON"
                                                + " l.loan_id=i.loan_id LEFT JOIN application_terms t"
                                                + " ON t.application_id=l.application_id WHERE"
                                                + " t.application_id IS NULL AND i.status IN"
                                                + " ('PENDING','PARTIALLY_PAID','OVERDUE')")) {
                    if (missing.getInt(1) > 0) {
                        throw new IllegalStateException(
                                "Legacy loan terms require review before scanning");
                    }
                }
                String sql =
                        """
                        SELECT i.installment_id, i.due_date, i.total_amount, i.paid_amount,
                               i.penalty_amount, i.penalty_weeks_assessed,
                               l.loan_id, l.member_id, lp.penalty_strategy, lp.penalty_rate
                        FROM installments i
                        JOIN loans l ON l.loan_id=i.loan_id
                        JOIN loan_applications a ON a.id=l.application_id
                        JOIN application_terms lp ON lp.application_id=a.id
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
                            int overdueWeeks =
                                    Math.max(
                                            1,
                                            (int) (ChronoUnit.DAYS.between(dueDate, asOfDate) / 7)
                                                    + 1);
                            int assessed = rs.getInt("penalty_weeks_assessed");
                            if (assessed < 0) {
                                throw new IllegalStateException(
                                        "Legacy penalty needs reconciliation for installment "
                                                + installmentId);
                            }
                            int chargeable =
                                    "GRACE".equals(rs.getString("penalty_strategy"))
                                            ? Math.max(0, overdueWeeks - 1)
                                            : overdueWeeks;
                            int newWeeks = Math.max(0, chargeable - assessed);
                            // Allocation is interest, principal, then penalty: penalty payments
                            // never
                            // reduce this base below zero. Previously assessed charges never
                            // decrease.
                            BigDecimal baseRemaining =
                                    rs.getBigDecimal("total_amount")
                                            .subtract(rs.getBigDecimal("paid_amount"))
                                            .max(BigDecimal.ZERO);
                            BigDecimal additional =
                                    baseRemaining
                                            .multiply(rs.getBigDecimal("penalty_rate"))
                                            .multiply(BigDecimal.valueOf(newWeeks))
                                            .movePointLeft(2)
                                            .max(BigDecimal.ZERO)
                                            .setScale(2, RoundingMode.HALF_UP);
                            BigDecimal penalty = rs.getBigDecimal("penalty_amount").add(additional);
                            try (PreparedStatement assessedUpdate =
                                    connection.prepareStatement(
                                            "UPDATE installments SET penalty_weeks_assessed=? WHERE"
                                                    + " installment_id=?")) {
                                assessedUpdate.setInt(1, Math.max(assessed, chargeable));
                                assessedUpdate.setInt(2, installmentId);
                                assessedUpdate.executeUpdate();
                            }

                            updateInstallment(connection, installmentId, penalty);
                            addPenaltyDifferenceToLoan(
                                    connection,
                                    rs.getInt("loan_id"),
                                    penalty.subtract(rs.getBigDecimal("penalty_amount")));
                            if (overdueWeeks >= 3) {
                                restrictMemberAndLoan(
                                        connection, rs.getInt("member_id"), rs.getInt("loan_id"));
                            }
                            new AuditLogRepository(connection)
                                    .save(
                                            "INSTALLMENT_OVERDUE",
                                            "INSTALLMENT",
                                            installmentId,
                                            "SYSTEM",
                                            "Overdue weeks="
                                                    + overdueWeeks
                                                    + "; penalty="
                                                    + penalty);
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
        try (PreparedStatement ps =
                connection.prepareStatement(
                        "UPDATE installments SET penalty_amount=?, status='OVERDUE' WHERE"
                                + " installment_id=?")) {
            ps.setBigDecimal(1, penalty);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private void restrictMemberAndLoan(Connection connection, int memberId, int loanId)
            throws Exception {
        try (PreparedStatement member =
                        connection.prepareStatement("UPDATE members SET eligible=0 WHERE id=?");
                PreparedStatement loan =
                        connection.prepareStatement(
                                "UPDATE loans SET status='DEFAULTED' WHERE loan_id=?")) {
            member.setInt(1, memberId);
            member.executeUpdate();
            loan.setInt(1, loanId);
            loan.executeUpdate();
        }
    }

    private void addPenaltyDifferenceToLoan(
            Connection connection, int loanId, BigDecimal difference) throws Exception {
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        try (PreparedStatement ps =
                connection.prepareStatement(
                        """
UPDATE loans SET outstanding_balance=ROUND(outstanding_balance+?,2) WHERE loan_id=?
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
