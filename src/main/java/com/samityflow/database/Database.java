package com.samityflow.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String url;

    public Database(String url) {
        this.url = url;
    }

    public static Database applicationDatabase() {
        return new Database("jdbc:sqlite:samityflow.db");
    }

    public Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public void initialize() {
        String[] statements = {
                "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, role TEXT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS samities (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, meeting_day TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)",
                "CREATE TABLE IF NOT EXISTS group_units (id INTEGER PRIMARY KEY AUTOINCREMENT, samity_id INTEGER NOT NULL, name TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(samity_id) REFERENCES samities(id), UNIQUE(samity_id, name))",
                "CREATE TABLE IF NOT EXISTS members (id INTEGER PRIMARY KEY AUTOINCREMENT, group_unit_id INTEGER NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL, eligible INTEGER NOT NULL DEFAULT 1, active INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(group_unit_id) REFERENCES group_units(id))",
                "CREATE TABLE IF NOT EXISTS loan_products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, interest_strategy TEXT NOT NULL, interest_rate REAL NOT NULL, penalty_strategy TEXT NOT NULL, penalty_rate REAL NOT NULL, duration_weeks INTEGER NOT NULL, required_guarantees INTEGER NOT NULL, weekly_savings REAL NOT NULL)",
                "CREATE TABLE IF NOT EXISTS loan_applications (id INTEGER PRIMARY KEY AUTOINCREMENT, member_id INTEGER NOT NULL, product_id INTEGER NOT NULL, amount REAL NOT NULL CHECK(amount > 0), purpose TEXT NOT NULL, status TEXT NOT NULL, officer_approved INTEGER NOT NULL DEFAULT 0, manager_comment TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(member_id) REFERENCES members(id), FOREIGN KEY(product_id) REFERENCES loan_products(id))",
                "CREATE TABLE IF NOT EXISTS guarantees (id INTEGER PRIMARY KEY AUTOINCREMENT, application_id INTEGER NOT NULL, guarantor_member_id INTEGER NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(application_id) REFERENCES loan_applications(id), FOREIGN KEY(guarantor_member_id) REFERENCES members(id), UNIQUE(application_id, guarantor_member_id))",
                "CREATE TABLE IF NOT EXISTS loans (loan_id INTEGER PRIMARY KEY AUTOINCREMENT, application_id INTEGER NOT NULL UNIQUE, member_id INTEGER NOT NULL, principal REAL NOT NULL CHECK(principal > 0), outstanding_balance REAL NOT NULL CHECK(outstanding_balance >= 0), status TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(application_id) REFERENCES loan_applications(id), FOREIGN KEY(member_id) REFERENCES members(id))",
                "CREATE TABLE IF NOT EXISTS installments (installment_id INTEGER PRIMARY KEY AUTOINCREMENT, loan_id INTEGER NOT NULL, installment_number INTEGER NOT NULL, due_date TEXT NOT NULL, principal_amount REAL NOT NULL DEFAULT 0 CHECK(principal_amount >= 0), interest_amount REAL NOT NULL DEFAULT 0 CHECK(interest_amount >= 0), total_amount REAL NOT NULL CHECK(total_amount >= 0), penalty_amount REAL NOT NULL DEFAULT 0 CHECK(penalty_amount >= 0), paid_amount REAL NOT NULL DEFAULT 0 CHECK(paid_amount >= 0), status TEXT NOT NULL DEFAULT 'PENDING', FOREIGN KEY(loan_id) REFERENCES loans(loan_id), UNIQUE(loan_id, installment_number))",
                "CREATE TABLE IF NOT EXISTS payments (payment_id INTEGER PRIMARY KEY AUTOINCREMENT, installment_id INTEGER NOT NULL, loan_id INTEGER NOT NULL, member_id INTEGER NOT NULL, amount REAL NOT NULL CHECK(amount <> 0), payment_reference TEXT NOT NULL, payment_date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, status TEXT NOT NULL, reversal_of_payment_id INTEGER, FOREIGN KEY(installment_id) REFERENCES installments(installment_id), FOREIGN KEY(loan_id) REFERENCES loans(loan_id), FOREIGN KEY(member_id) REFERENCES members(id), FOREIGN KEY(reversal_of_payment_id) REFERENCES payments(payment_id))",
                "CREATE TABLE IF NOT EXISTS savings_transactions (transaction_id INTEGER PRIMARY KEY AUTOINCREMENT, member_id INTEGER NOT NULL, transaction_type TEXT NOT NULL, amount REAL NOT NULL CHECK(amount > 0), transaction_reference TEXT NOT NULL, date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(member_id) REFERENCES members(id))",
                "CREATE TABLE IF NOT EXISTS audit_logs (audit_id INTEGER PRIMARY KEY AUTOINCREMENT, action TEXT NOT NULL, entity_type TEXT NOT NULL, entity_id INTEGER NOT NULL, username TEXT NOT NULL, details TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)"
        };

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
            ensurePaymentReversalColumn(connection);
            makeExistingReferencesUnique(connection, "payments", "payment_id", "payment_reference");
            makeExistingReferencesUnique(connection, "savings_transactions", "transaction_id", "transaction_reference");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_reference ON payments(payment_reference)");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_savings_reference ON savings_transactions(transaction_reference)");
        } catch (SQLException exception) {
            throw new RuntimeException("Could not initialize database", exception);
        }
    }

    private void makeExistingReferencesUnique(Connection connection, String table,
                                              String idColumn, String referenceColumn)
            throws SQLException {
        String sql = "UPDATE " + table + " SET " + referenceColumn + "=" + referenceColumn
                + " || '-' || " + idColumn + " WHERE " + idColumn + " NOT IN "
                + "(SELECT MIN(" + idColumn + ") FROM " + table + " GROUP BY " + referenceColumn + ")";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void ensurePaymentReversalColumn(Connection connection) throws SQLException {
        boolean hasColumn = false;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(payments)")) {
            while (rs.next()) {
                if ("reversal_of_payment_id".equalsIgnoreCase(rs.getString("name"))) {
                    hasColumn = true;
                    break;
                }
            }
        }

        if (!hasColumn) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE payments ADD COLUMN reversal_of_payment_id INTEGER REFERENCES payments(payment_id)");
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_reversal_of ON payments(reversal_of_payment_id) WHERE reversal_of_payment_id IS NOT NULL");
        }
    }

    public void seed() {
        String[] statements = {
                "INSERT OR IGNORE INTO users(id, name, role) VALUES (1, 'Rahim Officer', 'FIELD_OFFICER'), (2, 'Karim Manager', 'MANAGER')",
                "INSERT OR IGNORE INTO samities(id, name, meeting_day, active) VALUES (1, 'Shapla Samity', 'Sunday', 1)",
                "INSERT OR IGNORE INTO group_units(id, samity_id, name, active) VALUES (1, 1, 'Shapla Group A', 1)",
                "INSERT OR IGNORE INTO members(id, group_unit_id, name, phone, eligible, active) VALUES (1, 1, 'Amena Begum', '01700000001', 1, 1), (2, 1, 'Salma Akter', '01700000002', 1, 1), (3, 1, 'Rina Begum', '01700000003', 1, 1)",
                "INSERT OR IGNORE INTO loan_products(id, name, interest_strategy, interest_rate, penalty_strategy, penalty_rate, duration_weeks, required_guarantees, weekly_savings) VALUES (1, 'Agricultural Loan', 'AGRICULTURAL', 8, 'GRACE', 2, 24, 2, 100), (2, 'Business Loan', 'BUSINESS', 12, 'NORMAL', 2, 48, 2, 150), (3, 'Emergency Loan', 'EMERGENCY', 5, 'GRACE', 1, 12, 1, 50)"
        };

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.executeUpdate(sql);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not seed database", exception);
        }
    }
}
