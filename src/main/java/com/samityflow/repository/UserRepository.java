package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final Database database;
    public UserRepository(Database database) { this.database = database; }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection c = database.connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM users ORDER BY name")) {
            while (r.next()) users.add(new User(r.getInt("id"), r.getString("name"), r.getString("role")));
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Could not load users", e);
        }
    }
}
