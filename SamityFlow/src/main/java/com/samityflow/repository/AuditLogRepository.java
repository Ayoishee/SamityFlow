package com.samityflow.repository;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;



public class AuditLogRepository {


    private final Connection connection;



    public AuditLogRepository(
            Connection connection
    ){

        this.connection = connection;

    }






    public void save(
            String action,
            String entityType,
            int entityId,
            String user,
            String details
    ) throws SQLException {



        PreparedStatement ps =
                connection.prepareStatement("""
                INSERT INTO audit_logs(
                    action,
                    entity_type,
                    entity_id,
                    user_name,
                    details,
                    created_at
                )
                VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)
                """);



        ps.setString(
                1,
                action
        );


        ps.setString(
                2,
                entityType
        );


        ps.setInt(
                3,
                entityId
        );


        ps.setString(
                4,
                user
        );


        ps.setString(
                5,
                details
        );



        ps.executeUpdate();

    }


}