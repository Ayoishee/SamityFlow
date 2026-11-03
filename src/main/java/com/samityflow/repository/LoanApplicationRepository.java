package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.ApplicationStatus;
import com.samityflow.model.LoanApplication;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persists and queries LoanApplication records using JDBC. */
public class LoanApplicationRepository {
    private final Database database;
    private final Connection connection;

    public LoanApplicationRepository(Database database) {
        this.database = database;
        this.connection = null;
    }

    public LoanApplicationRepository(Connection connection) {
        this.database = null;
        this.connection = connection;
    }

    public LoanApplication saveNew(int memberId, int productId, double amount, String purpose) {
        String sql =
                "INSERT INTO loan_applications(member_id,product_id,amount,purpose,status)"
                        + " VALUES(?,?,?,?,?)";
        Connection currentConnection = openConnection();
        try (PreparedStatement statement =
                currentConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, memberId);
            statement.setInt(2, productId);
            statement.setDouble(3, amount);
            statement.setString(4, purpose);
            statement.setString(5, ApplicationStatus.DRAFT.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No application ID returned");
                }
                return new LoanApplication(
                        keys.getInt(1),
                        memberId,
                        productId,
                        amount,
                        purpose,
                        ApplicationStatus.DRAFT,
                        false,
                        "");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not create application", e);
        } finally {
            closeOwned(currentConnection);
        }
    }

    public void update(LoanApplication application) {
        String sql =
                "UPDATE loan_applications SET status=?, officer_approved=?, manager_comment=? WHERE"
                        + " id=?";
        Connection currentConnection = openConnection();
        try (PreparedStatement statement = currentConnection.prepareStatement(sql)) {
            statement.setString(1, application.getStatus().name());
            statement.setBoolean(2, application.isOfficerApproved());
            statement.setString(3, application.getManagerComment());
            statement.setInt(4, application.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update application", e);
        } finally {
            closeOwned(currentConnection);
        }
    }

    public Optional<LoanApplication> findById(int id) {
        Connection currentConnection = openConnection();
        try (PreparedStatement statement =
                currentConnection.prepareStatement("SELECT * FROM loan_applications WHERE id=?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load application", e);
        } finally {
            closeOwned(currentConnection);
        }
    }

    public List<LoanApplication> findAll() {
        List<LoanApplication> items = new ArrayList<>();
        Connection currentConnection = openConnection();
        try (Statement statement = currentConnection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "SELECT * FROM loan_applications ORDER BY id DESC")) {
            while (resultSet.next()) {
                items.add(map(resultSet));
            }
            return items;
        } catch (SQLException e) {
            throw new RuntimeException("Could not load applications", e);
        } finally {
            closeOwned(currentConnection);
        }
    }

    public int countPending() {
        return count(
                "SELECT COUNT(*) FROM loan_applications WHERE status NOT IN"
                        + " ('APPROVED','REJECTED')");
    }

    public int countAwaitingGuarantees() {
        return count("SELECT COUNT(*) FROM loan_applications WHERE status='GUARANTEE_PENDING'");
    }

    public int countApproved() {
        return count("SELECT COUNT(*) FROM loan_applications WHERE status='APPROVED'");
    }

    private int count(String sql) {
        Connection currentConnection = openConnection();
        try (Statement statement = currentConnection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        } finally {
            closeOwned(currentConnection);
        }
    }

    private LoanApplication map(ResultSet resultSet) throws SQLException {
        return new LoanApplication(
                resultSet.getInt("id"),
                resultSet.getInt("member_id"),
                resultSet.getInt("product_id"),
                resultSet.getDouble("amount"),
                resultSet.getString("purpose"),
                ApplicationStatus.valueOf(resultSet.getString("status")),
                resultSet.getBoolean("officer_approved"),
                resultSet.getString("manager_comment"));
    }

    private Connection openConnection() {
        try {
            return connection != null ? connection : database.connect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeOwned(Connection currentConnection) {
        if (connection == null) {
            try {
                currentConnection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
