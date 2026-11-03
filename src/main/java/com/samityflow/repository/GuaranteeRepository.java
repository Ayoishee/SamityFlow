package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.Guarantee;
import java.sql.*;

/** Persists and queries Guarantee records using JDBC. */
public class GuaranteeRepository {
    private final Database database;

    public GuaranteeRepository(Database database) {
        this.database = database;
    }

    public Guarantee save(int applicationId, int guarantorId) {
        String sql = "INSERT INTO guarantees(application_id,guarantor_member_id) VALUES(?,?)";
        try (Connection currentConnection = database.connect();
                PreparedStatement statement =
                        currentConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, applicationId);
            statement.setInt(2, guarantorId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return new Guarantee(keys.getInt(1), applicationId, guarantorId);
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                throw new IllegalArgumentException(
                        "This member already guaranteed the application");
            }
            throw new RuntimeException("Could not save guarantee", e);
        }
    }

    public boolean exists(int applicationId, int guarantorId) {
        try (Connection currentConnection = database.connect();
                PreparedStatement statement =
                        currentConnection.prepareStatement(
                                "SELECT COUNT(*) FROM guarantees WHERE application_id=? AND"
                                        + " guarantor_member_id=?")) {
            statement.setInt(1, applicationId);
            statement.setInt(2, guarantorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countForApplication(int applicationId) {
        try (Connection currentConnection = database.connect();
                PreparedStatement statement =
                        currentConnection.prepareStatement(
                                "SELECT COUNT(*) FROM guarantees WHERE application_id=?")) {
            statement.setInt(1, applicationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
