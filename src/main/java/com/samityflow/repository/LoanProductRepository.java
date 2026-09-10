package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.LoanProduct;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoanProductRepository {
    private final Database database;
    private final Connection connection;
    public LoanProductRepository(Database database) { this.database = database; this.connection = null; }
    public LoanProductRepository(Connection connection) { this.database = null; this.connection = connection; }

    public LoanProduct save(LoanProduct p) {
        validate(p);
        String sql = "INSERT INTO loan_products(name,interest_strategy,interest_rate,penalty_strategy,penalty_rate,duration_weeks,required_guarantees,weekly_savings) VALUES(?,?,?,?,?,?,?,?)";
        Connection c = openConnection();
        try (PreparedStatement s=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(s,p); s.executeUpdate(); try(ResultSet keys=s.getGeneratedKeys()){if(!keys.next())throw new SQLException("No product ID returned");return new LoanProduct(keys.getInt(1),p.name(),p.interestStrategy(),p.interestRate(),p.penaltyStrategy(),p.penaltyRate(),p.durationWeeks(),p.requiredGuarantees(),p.weeklySavings());}
        } catch(SQLException e){throw new RuntimeException("Could not save product",e);} finally { closeOwned(c); }
    }

    public List<LoanProduct> findAll() {
        List<LoanProduct> items=new ArrayList<>();
        Connection c = openConnection();
        try(Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM loan_products ORDER BY name")){while(r.next())items.add(map(r));return items;}
        catch(SQLException e){throw new RuntimeException("Could not load products",e);} finally { closeOwned(c); }
    }

    public Optional<LoanProduct> findById(int id) {
        Connection c = openConnection();
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM loan_products WHERE id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}
        catch(SQLException e){throw new RuntimeException("Could not load product",e);} finally { closeOwned(c); }
    }

    public void update(LoanProduct product) {
        validate(product);
        String sql="UPDATE loan_products SET name=?,interest_strategy=?,interest_rate=?,penalty_strategy=?,penalty_rate=?,duration_weeks=?,required_guarantees=?,weekly_savings=? WHERE id=?";
        Connection c=openConnection();
        try(PreparedStatement s=c.prepareStatement(sql)){set(s,product);s.setInt(9,product.id());s.executeUpdate();}
        catch(SQLException e){throw new RuntimeException("Could not update product",e);}finally{closeOwned(c);}
    }

    public void delete(int id) {
        Connection c=openConnection();
        try(PreparedStatement s=c.prepareStatement("DELETE FROM loan_products WHERE id=?")){s.setInt(1,id);s.executeUpdate();}
        catch(SQLException e){throw new RuntimeException("Could not delete a product used by applications",e);}finally{closeOwned(c);}
    }

    private void set(PreparedStatement s, LoanProduct p)throws SQLException{s.setString(1,p.name());s.setString(2,p.interestStrategy());s.setDouble(3,p.interestRate());s.setString(4,p.penaltyStrategy());s.setDouble(5,p.penaltyRate());s.setInt(6,p.durationWeeks());s.setInt(7,p.requiredGuarantees());s.setDouble(8,p.weeklySavings());}
    private LoanProduct map(ResultSet r)throws SQLException{return new LoanProduct(r.getInt("id"),r.getString("name"),r.getString("interest_strategy"),r.getDouble("interest_rate"),r.getString("penalty_strategy"),r.getDouble("penalty_rate"),r.getInt("duration_weeks"),r.getInt("required_guarantees"),r.getDouble("weekly_savings"));}
    private Connection openConnection() { try { return connection != null ? connection : database.connect(); } catch (SQLException e) { throw new RuntimeException(e); } }
    private void closeOwned(Connection c) { if (connection == null) try { c.close(); } catch (SQLException ignored) { } }
    private void validate(LoanProduct p) {
        if (p == null || p.name() == null || p.name().isBlank())
            throw new IllegalArgumentException("Product name is required");
        if (p.interestRate() < 0 || p.penaltyRate() < 0 || p.durationWeeks() <= 0
                || p.requiredGuarantees() < 0 || p.weeklySavings() < 0)
            throw new IllegalArgumentException("Product rates and savings cannot be negative, and duration must be positive");
        if (!List.of("AGRICULTURAL", "BUSINESS", "EMERGENCY").contains(p.interestStrategy()))
            throw new IllegalArgumentException("Unknown interest strategy");
        if (!List.of("NORMAL", "GRACE").contains(p.penaltyStrategy()))
            throw new IllegalArgumentException("Unknown penalty strategy");
    }
}
