package com.samityflow.database;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/** Additive, transactional migrations; uncertain legacy history is queued for human review. */
final class FinancialMigration {
    private FinancialMigration() {}

    static void apply(Connection connection) throws SQLException {
        try (TransactionScope transaction = new TransactionScope(connection);
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS schema_migrations (version INTEGER PRIMARY KEY,"
                            + " applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            boolean firstRun;
            try (ResultSet versions =
                    statement.executeQuery(
                            "SELECT COUNT(*) FROM schema_migrations WHERE version=1")) {
                firstRun = versions.getInt(1) == 0;
            }
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS migration_reviews (category TEXT NOT NULL,"
                            + " record_id INTEGER NOT NULL, details TEXT NOT NULL, PRIMARY"
                            + " KEY(category,record_id))");
            addColumn(
                    connection,
                    "savings_transactions",
                    "payment_id",
                    "INTEGER REFERENCES payments(payment_id)");
            addColumn(
                    connection,
                    "savings_transactions",
                    "reversal_of_transaction_id",
                    "INTEGER REFERENCES savings_transactions(transaction_id)");
            boolean newCents =
                    addColumn(connection, "savings_transactions", "amount_cents", "INTEGER");
            if (newCents) {
                statement.execute(
                        "INSERT OR IGNORE INTO migration_reviews SELECT 'SAVINGS_ROUNDING',"
                                + " transaction_id, 'Historical REAL amount requires rounding review'"
                                + " FROM savings_transactions WHERE ABS(amount * 100 - ROUND(amount *"
                                + " 100)) > 0.00001");
                statement.execute(
                        "UPDATE savings_transactions SET amount_cents=CAST(ROUND(amount*100) AS"
                                + " INTEGER)");
            }
            statement.execute(
                    "CREATE UNIQUE INDEX IF NOT EXISTS ux_savings_payment ON"
                            + " savings_transactions(payment_id) WHERE payment_id IS NOT NULL");
            statement.execute(
                    "CREATE UNIQUE INDEX IF NOT EXISTS ux_savings_reversal ON"
                            + " savings_transactions(reversal_of_transaction_id) WHERE"
                            + " reversal_of_transaction_id IS NOT NULL");
            if (firstRun) {
                statement.execute(
                        "INSERT OR IGNORE INTO migration_reviews SELECT 'SAVINGS_LINK',"
                                + " transaction_id, 'No proven payment relationship; reference text is"
                                + " insufficient' FROM savings_transactions WHERE payment_id IS NULL"
                                + " AND reversal_of_transaction_id IS NULL");
            }
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS application_terms (application_id INTEGER PRIMARY"
                            + " KEY REFERENCES loan_applications(id), name TEXT NOT NULL,"
                            + " interest_strategy TEXT NOT NULL, interest_rate REAL NOT NULL,"
                            + " penalty_strategy TEXT NOT NULL, penalty_rate REAL NOT NULL,"
                            + " duration_weeks INTEGER NOT NULL, required_guarantees INTEGER NOT NULL,"
                            + " weekly_savings REAL NOT NULL)");
            statement.execute(
                    "INSERT OR IGNORE INTO migration_reviews SELECT 'TERMS', id, 'Original agreed"
                            + " terms unknown; verify and insert application_terms before disbursement"
                            + " or penalty scan' FROM loan_applications WHERE id NOT IN (SELECT"
                            + " application_id FROM application_terms)");
            // Capture earlier than approval so even in-flight guarantees cannot change under an
            // applicant.
            statement.execute(
                    "CREATE TRIGGER IF NOT EXISTS snapshot_application_terms AFTER INSERT ON"
                            + " loan_applications BEGIN INSERT INTO application_terms SELECT"
                            + " NEW.id,name,interest_strategy,interest_rate,penalty_strategy,penalty_rate,duration_weeks,required_guarantees,weekly_savings"
                            + " FROM loan_products WHERE id=NEW.product_id; END");
            statement.execute(
                    "CREATE TRIGGER IF NOT EXISTS preserve_application_terms BEFORE DELETE ON"
                            + " application_terms BEGIN SELECT RAISE(ABORT,'Application terms preserve"
                            + " financial history'); END");
            statement.execute(
                    "INSERT OR IGNORE INTO migration_reviews SELECT"
                            + " 'PAYMENT_REFERENCE',payment_id,'Duplicate historical reference"
                            + " preserved' FROM payments WHERE payment_reference IN (SELECT"
                            + " payment_reference FROM payments GROUP BY payment_reference HAVING"
                            + " COUNT(*)>1)");
            statement.execute(
                    "INSERT OR IGNORE INTO migration_reviews SELECT"
                            + " 'SAVINGS_REFERENCE',transaction_id,'Duplicate historical reference"
                            + " preserved' FROM savings_transactions WHERE transaction_reference IN"
                            + " (SELECT transaction_reference FROM savings_transactions GROUP BY"
                            + " transaction_reference HAVING COUNT(*)>1)");
            statement.execute(
                    "CREATE TRIGGER IF NOT EXISTS immutable_application_terms BEFORE UPDATE ON"
                            + " application_terms BEGIN SELECT RAISE(ABORT,'Application terms are"
                            + " immutable'); END");
            for (String column : new String[] {"principal_paid", "interest_paid", "penalty_paid"}) {
                if (addColumn(connection, "payments", column, "REAL")) {
                    statement.execute(
                            "INSERT OR IGNORE INTO migration_reviews SELECT 'ALLOCATION',"
                                    + " payment_id, 'Historical payment allocation unavailable;"
                                    + " cumulative base-first policy applies prospectively' FROM"
                                    + " payments");
                }
            }
            if (addColumn(
                    connection,
                    "installments",
                    "penalty_weeks_assessed",
                    "INTEGER NOT NULL DEFAULT 0")) {
                statement.execute(
                        "INSERT OR IGNORE INTO migration_reviews SELECT 'PENALTY', installment_id,"
                                + " 'Historical penalty period unknown; manual reconciliation required"
                                + " before scan' FROM installments WHERE penalty_amount > 0");
                statement.execute(
                        "UPDATE installments SET penalty_weeks_assessed=-1 WHERE penalty_amount >"
                                + " 0");
            }
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS schema_migrations (version INTEGER PRIMARY KEY,"
                            + " applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("INSERT OR IGNORE INTO schema_migrations(version) VALUES(1)");
            transaction.complete();
        }
    }

    private static boolean addColumn(Connection connection, String table, String name, String type)
            throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }
        if (columns.contains(name)) {
            return false;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + name + " " + type);
        }
        return true;
    }
}
