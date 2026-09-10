package com.samityflow.service;

import com.samityflow.database.Database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private final Database database;

    public ReportService(Database database) {
        this.database = database;
    }

    public PortfolioSummary portfolioSummary() {
        try (Connection connection = database.connect()) {
            int activeLoans = integer(connection,
                    "SELECT COUNT(*) FROM loans WHERE status IN ('ACTIVE','DEFAULTED')");
            BigDecimal outstanding = decimal(connection,
                    "SELECT COALESCE(SUM(outstanding_balance),0) FROM loans WHERE status IN ('ACTIVE','DEFAULTED')");
            int overdue = integer(connection,
                    "SELECT COUNT(*) FROM installments WHERE status='OVERDUE'");
            BigDecimal savings = decimal(connection, """
                    SELECT COALESCE(SUM(CASE WHEN transaction_type='DEPOSIT' THEN amount ELSE -amount END),0)
                    FROM savings_transactions
                    """);
            return new PortfolioSummary(activeLoans, outstanding, overdue, savings);
        } catch (Exception exception) {
            throw new RuntimeException("Could not create portfolio report", exception);
        }
    }

    public CollectionSummary collectionSummary() {
        try (Connection connection = database.connect()) {
            BigDecimal expected = decimal(connection,
                    "SELECT COALESCE(SUM(total_amount + penalty_amount),0) FROM installments");
            BigDecimal collected = decimal(connection,
                    "SELECT COALESCE(SUM(amount),0) FROM payments");
            double rate = expected.signum() == 0 ? 0
                    : collected.multiply(BigDecimal.valueOf(100))
                    .divide(expected, 2, java.math.RoundingMode.HALF_UP).doubleValue();
            return new CollectionSummary(expected, collected, rate);
        } catch (Exception exception) {
            throw new RuntimeException("Could not create collection report", exception);
        }
    }

    public List<MemberFinancialRow> memberFinancialReport() {
        String sql = """
                SELECT m.id, m.name,
                       COALESCE((SELECT SUM(l.outstanding_balance) FROM loans l
                                 WHERE l.member_id=m.id AND l.status IN ('ACTIVE','DEFAULTED')),0) AS outstanding,
                       COALESCE((SELECT SUM(CASE WHEN s.transaction_type='DEPOSIT' THEN s.amount ELSE -s.amount END)
                                 FROM savings_transactions s WHERE s.member_id=m.id),0) AS savings
                FROM members m ORDER BY m.name
                """;
        List<MemberFinancialRow> result = new ArrayList<>();
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new MemberFinancialRow(rs.getInt("id"), rs.getString("name"),
                        rs.getBigDecimal("outstanding"), rs.getBigDecimal("savings")));
            }
            return result;
        } catch (Exception exception) {
            throw new RuntimeException("Could not create member report", exception);
        }
    }

    public List<InstallmentRow> outstandingInstallments() {
        String sql = """
                SELECT i.installment_id, i.loan_id, l.member_id, m.name, i.installment_number,
                       i.due_date, i.total_amount, i.paid_amount, i.penalty_amount, i.status
                FROM installments i
                JOIN loans l ON l.loan_id=i.loan_id
                JOIN members m ON m.id=l.member_id
                WHERE i.status IN ('PENDING','PARTIALLY_PAID','OVERDUE')
                ORDER BY i.due_date, i.installment_number
                """;
        List<InstallmentRow> result = new ArrayList<>();
        try (Connection connection = database.connect(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new InstallmentRow(rs.getInt("installment_id"), rs.getInt("loan_id"),
                        rs.getInt("member_id"), rs.getString("name"), rs.getInt("installment_number"),
                        LocalDate.parse(rs.getString("due_date")), rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("paid_amount"), rs.getBigDecimal("penalty_amount"),
                        rs.getString("status")));
            }
            return result;
        } catch (Exception exception) {
            throw new RuntimeException("Could not load installments", exception);
        }
    }

    public List<PaymentRow> recentPayments() {
        List<PaymentRow> result = new ArrayList<>();
        try (Connection connection = database.connect(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT payment_id, member_id, amount, payment_reference, status
                     FROM payments ORDER BY payment_id DESC LIMIT 50
                     """)) {
            while (rs.next()) result.add(new PaymentRow(rs.getInt("payment_id"), rs.getInt("member_id"),
                    rs.getBigDecimal("amount"), rs.getString("payment_reference"), rs.getString("status")));
            return result;
        } catch (Exception exception) {
            throw new RuntimeException("Could not load payments", exception);
        }
    }

    private int integer(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) { return rs.getInt(1); }
    }

    private BigDecimal decimal(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) { return rs.getBigDecimal(1); }
    }

    public record PortfolioSummary(int activeLoans, BigDecimal outstanding,
                                   int overdueInstallments, BigDecimal savingsBalance) { }
    public record CollectionSummary(BigDecimal expected, BigDecimal collected, double ratePercent) { }
    public record MemberFinancialRow(int memberId, String memberName,
                                     BigDecimal outstanding, BigDecimal savings) {
        @Override public String toString() {
            return memberName + " | loan: " + outstanding + " | savings: " + savings;
        }
    }
    public record InstallmentRow(int installmentId, int loanId, int memberId, String memberName,
                                 int number, LocalDate dueDate, BigDecimal total, BigDecimal paid,
                                 BigDecimal penalty, String status) {
        public BigDecimal remaining() { return total.add(penalty).subtract(paid); }
        @Override public String toString() {
            return memberName + " | loan #" + loanId + " | installment " + number
                    + " | due " + dueDate + " | remaining " + remaining() + " | " + status;
        }
    }
    public record PaymentRow(int paymentId, int memberId, BigDecimal amount,
                             String reference, String status) {
        @Override public String toString() {
            return "Payment #" + paymentId + " | " + reference + " | " + amount + " | " + status;
        }
    }

    public List<AuditRow> recentAuditHistory() {
        List<AuditRow> result = new ArrayList<>();
        try (Connection connection = database.connect(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT audit_id, action, entity_type, entity_id, username, details, created_at
                     FROM audit_logs ORDER BY audit_id DESC LIMIT 100
                     """)) {
            while (rs.next()) result.add(new AuditRow(rs.getInt("audit_id"), rs.getString("action"),
                    rs.getString("entity_type"), rs.getInt("entity_id"), rs.getString("username"),
                    rs.getString("details"), rs.getString("created_at")));
            return result;
        } catch (Exception exception) {
            throw new RuntimeException("Could not load audit history", exception);
        }
    }

    public record AuditRow(int auditId, String action, String entityType, int entityId,
                           String username, String details, String createdAt) {
        @Override public String toString() {
            return createdAt + " | " + action + " | " + entityType + " #" + entityId
                    + " | " + username + " | " + details;
        }
    }
}
