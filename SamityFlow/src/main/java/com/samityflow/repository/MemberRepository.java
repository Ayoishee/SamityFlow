package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.Member;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberRepository {
    private final Database database;
    public MemberRepository(Database database) { this.database = database; }

    public Member save(int groupId, String name, String phone) {
        String sql = "INSERT INTO members(group_unit_id, name, phone, eligible, active) VALUES (?, ?, ?, 1, 1)";
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, groupId); s.setString(2, name); s.setString(3, phone); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { return new Member(keys.getInt(1), groupId, name, phone, true, true); }
        } catch (SQLException e) { throw new RuntimeException("Could not save member", e); }
    }

    public List<Member> findAll() {
        List<Member> items = new ArrayList<>();
        try (Connection c = database.connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM members ORDER BY name")) {
            while (r.next()) items.add(map(r)); return items;
        } catch (SQLException e) { throw new RuntimeException("Could not load members", e); }
    }

    public Optional<Member> findById(int id) {
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement("SELECT * FROM members WHERE id=?")) {
            s.setInt(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(map(r)) : Optional.empty(); }
        } catch (SQLException e) { throw new RuntimeException("Could not load member", e); }
    }

    public boolean belongsToActiveGroup(int memberId) {
        String sql = "SELECT COUNT(*) FROM members m JOIN group_units g ON m.group_unit_id=g.id JOIN samities s ON g.samity_id=s.id WHERE m.id=? AND m.active=1 AND g.active=1 AND s.active=1";
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, memberId); try (ResultSet r = s.executeQuery()) { return r.getInt(1) == 1; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int count() {
        try (Connection c = database.connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM members")) { return r.getInt(1); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Member map(ResultSet r) throws SQLException {
        return new Member(r.getInt("id"), r.getInt("group_unit_id"), r.getString("name"), r.getString("phone"), r.getBoolean("eligible"), r.getBoolean("active"));
    }
}
