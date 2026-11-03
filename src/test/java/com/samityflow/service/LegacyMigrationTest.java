package com.samityflow.service;

import static org.junit.jupiter.api.Assertions.*;

import com.samityflow.database.Database;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the actual pre-migration schema with ambiguous financial history. */
class LegacyMigrationTest {
    @TempDir Path directory;

    @Test
    void migrationIsRepeatableAndPreservesAmbiguousHistory() throws Exception {
        Database database = new Database("jdbc:sqlite:" + directory.resolve("legacy.db"));
        try (Connection connection = database.connect();
                Statement statement = connection.createStatement()) {
            String schema =
                    new String(
                            getClass().getResourceAsStream("/legacy-schema.sql").readAllBytes(),
                            StandardCharsets.UTF_8);
            for (String sql : schema.split("\n")) {
                statement.execute(sql);
            }
        }
        database.seed();
        try (Connection connection = database.connect();
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "INSERT INTO loan_applications(id,member_id,product_id,amount,purpose,status)"
                            + " VALUES(1,1,3,100,'Legacy','APPROVED')");
            statement.execute(
                    "INSERT INTO"
                            + " loans(loan_id,application_id,member_id,principal,outstanding_balance,status)"
                            + " VALUES(1,1,1,100,95,'ACTIVE')");
            statement.execute(
                    "INSERT INTO"
                            + " installments(loan_id,installment_number,due_date,total_amount,penalty_amount,paid_amount)"
                            + " VALUES(1,1,'2020-01-01',100,5,10)");
            statement.execute(
                    "INSERT INTO"
                            + " payments(installment_id,loan_id,member_id,amount,payment_reference,status)"
                            + " VALUES(1,1,1,10,'SAME','COMPLETED')");
            statement.execute(
                    "INSERT INTO"
                            + " savings_transactions(member_id,transaction_type,amount,transaction_reference)"
                            + " VALUES(1,'DEPOSIT',20.125,'SAME'),(2,'DEPOSIT',30,'SAME')");
            statement.execute(
                    "INSERT INTO audit_logs(action,entity_type,entity_id,username,details)"
                            + " VALUES('LEGACY','PAYMENT',1,'SYSTEM','Original history')");
        }
        database.initialize();
        database.initialize();
        try (Connection connection = database.connect();
                Statement statement = connection.createStatement()) {
            assertEquals(
                    2,
                    number(
                            statement,
                            "SELECT COUNT(*) FROM savings_transactions WHERE"
                                    + " transaction_reference='SAME'"));
            assertEquals(
                    20.125,
                    number(
                            statement,
                            "SELECT amount FROM savings_transactions WHERE transaction_id=1"));
            assertEquals(
                    2013,
                    number(
                            statement,
                            "SELECT amount_cents FROM savings_transactions WHERE"
                                    + " transaction_id=1"));
            assertEquals(
                    0,
                    number(
                            statement,
                            "SELECT COUNT(*) FROM savings_transactions WHERE payment_id IS NOT"
                                    + " NULL"));
            assertEquals(0, number(statement, "SELECT COUNT(*) FROM application_terms"));
            assertEquals(
                    1,
                    number(
                            statement,
                            "SELECT COUNT(*) FROM migration_reviews WHERE category='TERMS'"));
            assertEquals(
                    2,
                    number(
                            statement,
                            "SELECT COUNT(*) FROM migration_reviews WHERE"
                                    + " category='SAVINGS_LINK'"));
            assertEquals(1, number(statement, "SELECT COUNT(*) FROM audit_logs"));
            assertEquals(-1, number(statement, "SELECT penalty_weeks_assessed FROM installments"));
        }
        assertThrows(
                IllegalStateException.class,
                () -> new OverdueService(database).scan(java.time.LocalDate.now()));
    }

    double number(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            return result.getDouble(1);
        }
    }
}
