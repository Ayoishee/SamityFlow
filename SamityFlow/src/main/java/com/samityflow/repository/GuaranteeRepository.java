package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.Guarantee;
import java.sql.*;

public class GuaranteeRepository {
    private final Database database;
    public GuaranteeRepository(Database database){this.database=database;}

    public Guarantee save(int applicationId,int guarantorId){
        String sql="INSERT INTO guarantees(application_id,guarantor_member_id) VALUES(?,?)";
        try(Connection c=database.connect();PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setInt(1,applicationId);s.setInt(2,guarantorId);s.executeUpdate();try(ResultSet keys=s.getGeneratedKeys()){return new Guarantee(keys.getInt(1),applicationId,guarantorId);}}
        catch(SQLException e){if(e.getMessage().contains("UNIQUE"))throw new IllegalArgumentException("This member already guaranteed the application");throw new RuntimeException("Could not save guarantee",e);}
    }

    public boolean exists(int applicationId,int guarantorId){
        try(Connection c=database.connect();PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM guarantees WHERE application_id=? AND guarantor_member_id=?")){s.setInt(1,applicationId);s.setInt(2,guarantorId);try(ResultSet r=s.executeQuery()){return r.getInt(1)>0;}}
        catch(SQLException e){throw new RuntimeException(e);}
    }

    public int countForApplication(int applicationId){
        try(Connection c=database.connect();PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM guarantees WHERE application_id=?")){s.setInt(1,applicationId);try(ResultSet r=s.executeQuery()){return r.getInt(1);}}
        catch(SQLException e){throw new RuntimeException(e);}
    }
}
