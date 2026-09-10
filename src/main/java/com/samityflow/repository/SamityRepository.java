package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.Samity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SamityRepository {
    private final Database database;

    public SamityRepository(Database database) { this.database = database; }

    public Samity save(String name, String meetingDay) {
        validate(name, meetingDay);
        name = name.trim();
        meetingDay = meetingDay.trim();
        String sql = "INSERT INTO samities(name, meeting_day, active) VALUES (?, ?, 1)";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, meetingDay);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No samity ID returned");
                return new Samity(keys.getInt(1), name, meetingDay, true);
            }
        } catch (SQLException exception) { throw new RuntimeException("Could not save samity", exception); }
    }

    public List<Samity> findAll() {
        List<Samity> items = new ArrayList<>();
        try (Connection connection = database.connect(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM samities ORDER BY name")) {
            while (result.next()) items.add(map(result));
            return items;
        } catch (SQLException exception) { throw new RuntimeException("Could not load samities", exception); }
    }

    public void setActive(int id, boolean active) {
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement("UPDATE samities SET active=? WHERE id=?")) {
            statement.setBoolean(1, active); statement.setInt(2, id); statement.executeUpdate();
        } catch (SQLException exception) { throw new RuntimeException("Could not update samity", exception); }
    }

    public void update(int id, String name, String meetingDay) {
        validate(name, meetingDay);
        execute("UPDATE samities SET name=?, meeting_day=? WHERE id=?", name.trim(), meetingDay.trim(), id);
    }

    public void delete(int id) {
        execute("DELETE FROM samities WHERE id=?", id);
    }

    public int countActive() { return count("SELECT COUNT(*) FROM samities WHERE active=1"); }

    private int count(String sql) {
        try (Connection connection = database.connect(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            return result.getInt(1);
        } catch (SQLException exception) { throw new RuntimeException(exception); }
    }

    private Samity map(ResultSet result) throws SQLException {
        return new Samity(result.getInt("id"), result.getString("name"), result.getString("meeting_day"), result.getBoolean("active"));
    }

    private void validate(String name, String meetingDay) {
        if (name == null || name.isBlank() || meetingDay == null || meetingDay.isBlank())
            throw new IllegalArgumentException("Samity name and meeting day are required");
    }

    private void execute(String sql, Object... values) {
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]);
            s.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Could not change samity. Remove dependent groups first.", e); }
    }
}
