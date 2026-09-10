package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.GroupUnit;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupUnitRepository {
    private final Database database;
    public GroupUnitRepository(Database database) { this.database = database; }

    public GroupUnit save(int samityId, String name) {
        validate(samityId, name);
        name = name.trim();
        String sql = "INSERT INTO group_units(samity_id, name, active) VALUES (?, ?, 1)";
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, samityId); s.setString(2, name); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if(!keys.next())throw new SQLException("No group ID returned");return new GroupUnit(keys.getInt(1), samityId, name, true); }
        } catch (SQLException e) { throw new RuntimeException("Could not save group", e); }
    }

    public List<GroupUnit> findAll() {
        List<GroupUnit> items = new ArrayList<>();
        try (Connection c = database.connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM group_units ORDER BY name")) {
            while (r.next()) items.add(new GroupUnit(r.getInt("id"), r.getInt("samity_id"), r.getString("name"), r.getBoolean("active")));
            return items;
        } catch (SQLException e) { throw new RuntimeException("Could not load groups", e); }
    }

    public void update(int id, int samityId, String name) {
        validate(samityId, name);
        execute("UPDATE group_units SET samity_id=?, name=? WHERE id=?", samityId, name.trim(), id);
    }

    public void setActive(int id, boolean active) {
        execute("UPDATE group_units SET active=? WHERE id=?", active, id);
    }

    public void delete(int id) {
        execute("DELETE FROM group_units WHERE id=?", id);
    }

    private void execute(String sql, Object... values) {
        try (Connection c=database.connect();PreparedStatement s=c.prepareStatement(sql)) {
            for(int i=0;i<values.length;i++)s.setObject(i+1,values[i]);s.executeUpdate();
        } catch(SQLException e){throw new RuntimeException("Could not change group. Remove dependent members first.",e);}
    }

    private void validate(int samityId, String name) {
        if (samityId <= 0 || name == null || name.isBlank())
            throw new IllegalArgumentException("A samity and group name are required");
    }
}
