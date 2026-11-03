package com.samityflow.repository;

import java.sql.*;

/** Persists and queries AuditLog records using JDBC. */
public class AuditLogRepository {

    private final Connection connection;

    public AuditLogRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(
            String action, String entityType, int entityId, String username, String details)
            throws SQLException {

        try (PreparedStatement preparedStatement =
                connection.prepareStatement(
                        """
                            INSERT INTO audit_logs
                            (action, entity_type, entity_id, username, details)
                            VALUES(?,?,?,?,?)
                        """)) {

            preparedStatement.setString(1, action);
            preparedStatement.setString(2, entityType);
            preparedStatement.setInt(3, entityId);
            preparedStatement.setString(4, username);
            preparedStatement.setString(5, details);

            preparedStatement.executeUpdate();
        }
    }
}
