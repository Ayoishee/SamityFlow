package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.Member;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persists and queries Member records using JDBC. */
public class MemberRepository {
    private final Database database;

    public MemberRepository(Database database) {
        this.database = database;
    }

    public Member save(int groupId, String name, String phone) {
        validate(groupId, name, phone);
        name = name.trim();
        phone = phone.trim();
        String sql =
                "INSERT INTO members(group_unit_id, name, phone, eligible, active) VALUES (?, ?, ?,"
                        + " 1, 1)";
        try (Connection currentConnection = database.connect();
                PreparedStatement statement =
                        currentConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, groupId);
            statement.setString(2, name);
            statement.setString(3, phone);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No member ID returned");
                }
                return new Member(keys.getInt(1), groupId, name, phone, true, true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save member", e);
        }
    }

    public List<Member> findAll() {
        List<Member> items = new ArrayList<>();
        try (Connection currentConnection = database.connect();
                Statement statement = currentConnection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery("SELECT * FROM members ORDER BY name")) {
            while (resultSet.next()) {
                items.add(map(resultSet));
            }
            return items;
        } catch (SQLException e) {
            throw new RuntimeException("Could not load members", e);
        }
    }

    public Optional<Member> findById(int id) {
        try (Connection currentConnection = database.connect();
                PreparedStatement statement =
                        currentConnection.prepareStatement("SELECT * FROM members WHERE id=?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load member", e);
        }
    }

    public boolean belongsToActiveGroup(int memberId) {
        String sql =
                "SELECT COUNT(*) FROM members m JOIN group_units g ON m.group_unit_id=g.id JOIN"
                        + " samities s ON g.samity_id=s.id WHERE m.id=? AND m.active=1 AND g.active=1"
                        + " AND s.active=1";
        try (Connection currentConnection = database.connect();
                PreparedStatement statement = currentConnection.prepareStatement(sql)) {
            statement.setInt(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.getInt(1) == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int count() {
        try (Connection currentConnection = database.connect();
                Statement statement = currentConnection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM members")) {
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(
            int id, int groupId, String name, String phone, boolean eligible, boolean active) {
        validate(groupId, name, phone);
        String sql =
                "UPDATE members SET group_unit_id=?,name=?,phone=?,eligible=?,active=? WHERE id=?";
        try (Connection currentConnection = database.connect();
                PreparedStatement statement = currentConnection.prepareStatement(sql)) {
            statement.setInt(1, groupId);
            statement.setString(2, name.trim());
            statement.setString(3, phone.trim());
            statement.setBoolean(4, eligible);
            statement.setBoolean(5, active);
            statement.setInt(6, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update member", e);
        }
    }

    public void delete(int id) {
        try (Connection currentConnection = database.connect();
                PreparedStatement statement =
                        currentConnection.prepareStatement("DELETE FROM members WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete member with loan or guarantee history", e);
        }
    }

    private Member map(ResultSet resultSet) throws SQLException {
        return new Member(
                resultSet.getInt("id"),
                resultSet.getInt("group_unit_id"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getBoolean("eligible"),
                resultSet.getBoolean("active"));
    }

    private void validate(int groupId, String name, String phone) {
        if (groupId <= 0 || name == null || name.isBlank() || phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("A group, member name and phone are required");
        }
    }
}
