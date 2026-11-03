package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Persists and queries User records using JDBC. */
public class UserRepository {
    private final Database database;

    public UserRepository(Database database) {
        this.database = database;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection currentConnection = database.connect();
                Statement statement = currentConnection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM users ORDER BY name")) {
            while (resultSet.next()) {
                users.add(
                        new User(
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getString("role")));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Could not load users", e);
        }
    }
}
