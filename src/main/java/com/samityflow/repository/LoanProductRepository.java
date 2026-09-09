package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.LoanProduct;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoanProductRepository {
    private final Database database;
    public LoanProductRepository(Database database) { this.database = database; }

    public LoanProduct save(LoanProduct p) {
        String sql = "INSERT INTO loan_products(name,interest_strategy,interest_rate,penalty_strategy,penalty_rate,duration_weeks,required_guarantees,weekly_savings) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection c=database.connect(); PreparedStatement s=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(s,p); s.executeUpdate(); try(ResultSet keys=s.getGeneratedKeys()){return new LoanProduct(keys.getInt(1),p.name(),p.interestStrategy(),p.interestRate(),p.penaltyStrategy(),p.penaltyRate(),p.durationWeeks(),p.requiredGuarantees(),p.weeklySavings());}
        } catch(SQLException e){throw new RuntimeException("Could not save product",e);}
    }

    public List<LoanProduct> findAll() {
        List<LoanProduct> items=new ArrayList<>();
        try(Connection c=database.connect(); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM loan_products ORDER BY name")){while(r.next())items.add(map(r));return items;}
        catch(SQLException e){throw new RuntimeException("Could not load products",e);}
    }

    public Optional<LoanProduct> findById(int id) {
        try(Connection c=database.connect(); PreparedStatement s=c.prepareStatement("SELECT * FROM loan_products WHERE id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}
        catch(SQLException e){throw new RuntimeException("Could not load product",e);}
    }

    private void set(PreparedStatement s, LoanProduct p)throws SQLException{s.setString(1,p.name());s.setString(2,p.interestStrategy());s.setDouble(3,p.interestRate());s.setString(4,p.penaltyStrategy());s.setDouble(5,p.penaltyRate());s.setInt(6,p.durationWeeks());s.setInt(7,p.requiredGuarantees());s.setDouble(8,p.weeklySavings());}
    private LoanProduct map(ResultSet r)throws SQLException{return new LoanProduct(r.getInt("id"),r.getString("name"),r.getString("interest_strategy"),r.getDouble("interest_rate"),r.getString("penalty_strategy"),r.getDouble("penalty_rate"),r.getInt("duration_weeks"),r.getInt("required_guarantees"),r.getDouble("weekly_savings"));}
}
