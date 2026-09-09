package com.samityflow.repository;

import java.sql.*;

public class AuditLogRepository {

    private final Connection connection;

    public AuditLogRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(String action, String entityType,
                     int entityId, String username,
                     String details) throws SQLException {

        PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO audit_logs
            (action, entity_type, entity_id, username, details)
            VALUES(?,?,?,?,?)
        """);

        ps.setString(1, action);
        ps.setString(2, entityType);
        ps.setInt(3, entityId);
        ps.setString(4, username);
        ps.setString(5, details);

        ps.executeUpdate();
    }
}
