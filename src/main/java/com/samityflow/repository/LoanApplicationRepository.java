package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.ApplicationStatus;
import com.samityflow.model.LoanApplication;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoanApplicationRepository {
    private final Database database;
    private final Connection connection;

    public LoanApplicationRepository(Database database) { this.database = database; this.connection = null; }
    public LoanApplicationRepository(Connection connection) { this.database = null; this.connection = connection; }

    public LoanApplication saveNew(int memberId, int productId, double amount, String purpose) {
        String sql="INSERT INTO loan_applications(member_id,product_id,amount,purpose,status) VALUES(?,?,?,?,?)";
        Connection c = openConnection();
        try(PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,memberId);s.setInt(2,productId);s.setDouble(3,amount);s.setString(4,purpose);s.setString(5,ApplicationStatus.DRAFT.name());s.executeUpdate();
            try(ResultSet keys=s.getGeneratedKeys()){if(!keys.next())throw new SQLException("No application ID returned");return new LoanApplication(keys.getInt(1),memberId,productId,amount,purpose,ApplicationStatus.DRAFT,false,"");}
        }catch(SQLException e){throw new RuntimeException("Could not create application",e);} finally { closeOwned(c); }
    }

    public void update(LoanApplication application) {
        String sql="UPDATE loan_applications SET status=?, officer_approved=?, manager_comment=? WHERE id=?";
        Connection c = openConnection();
        try(PreparedStatement s=c.prepareStatement(sql)){
            s.setString(1,application.getStatus().name());s.setBoolean(2,application.isOfficerApproved());s.setString(3,application.getManagerComment());s.setInt(4,application.getId());s.executeUpdate();
        }catch(SQLException e){throw new RuntimeException("Could not update application",e);} finally { closeOwned(c); }
    }

    public Optional<LoanApplication> findById(int id) {
        Connection c = openConnection();
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM loan_applications WHERE id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}
        catch(SQLException e){throw new RuntimeException("Could not load application",e);} finally { closeOwned(c); }
    }

    public List<LoanApplication> findAll() {
        List<LoanApplication> items=new ArrayList<>();
        Connection c = openConnection();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM loan_applications ORDER BY id DESC")){while(r.next())items.add(map(r));return items;}
        catch(SQLException e){throw new RuntimeException("Could not load applications",e);} finally { closeOwned(c); }
    }

    public int countPending() {return count("SELECT COUNT(*) FROM loan_applications WHERE status NOT IN ('APPROVED','REJECTED')");}
    public int countAwaitingGuarantees(){return count("SELECT COUNT(*) FROM loan_applications WHERE status='GUARANTEE_PENDING'");}
    public int countApproved(){return count("SELECT COUNT(*) FROM loan_applications WHERE status='APPROVED'");}
    private int count(String sql){try(Connection c=database.connect();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){return r.getInt(1);}catch(SQLException e){throw new RuntimeException(e);}}
    private LoanApplication map(ResultSet r)throws SQLException{return new LoanApplication(r.getInt("id"),r.getInt("member_id"),r.getInt("product_id"),r.getDouble("amount"),r.getString("purpose"),ApplicationStatus.valueOf(r.getString("status")),r.getBoolean("officer_approved"),r.getString("manager_comment"));}
    private Connection openConnection() { try { return connection != null ? connection : database.connect(); } catch (SQLException e) { throw new RuntimeException(e); } }
    private void closeOwned(Connection c) { if (connection == null) try { c.close(); } catch (SQLException ignored) { } }
}
