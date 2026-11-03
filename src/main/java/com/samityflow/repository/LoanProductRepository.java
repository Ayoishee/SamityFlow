package com.samityflow.repository;

import com.samityflow.database.Database;
import com.samityflow.model.LoanProduct;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persists and queries LoanProduct records using JDBC. */
public class LoanProductRepository {
    private final Database database;
    private final Connection connection;

    public LoanProductRepository(Database database) {
        this.database = database;
        this.connection = null;
    }

    public LoanProductRepository(Connection connection) {
        this.database = null;
        this.connection = connection;
    }

    public LoanProduct save(LoanProduct p) {
        validate(p);
        String sql =
                "INSERT INTO"
                        + " loan_products(name,interest_strategy,interest_rate,penalty_strategy,penalty_rate,duration_weeks,required_guarantees,weekly_savings)"
                        + " VALUES(?,?,?,?,?,?,?,?)";
        Connection currentConnection = openConnection();
        try (PreparedStatement statement =
                currentConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(statement, p);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No product ID returned");
                }
                return new LoanProduct(
                        keys.getInt(1),
                        p.name(),
                        p.interestStrategy(),
                        p.interestRate(),
                        p.penaltyStrategy(),
                        p.penaltyRate(),
                        p.durationWeeks(),
                        p.requiredGuarantees(),
                        p.weeklySavings());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save product", e);
        } finally {
            closeOwned(currentConnection);
        }
    }

    public List<LoanProduct> findAll() {
        List<LoanProduct> items = new ArrayList<>();
        Connection currentConnection = openConnection();
        try (Statement statement = currentConnection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery("SELECT * FROM loan_products ORDER BY name")) {
            while (resultSet.next()) {
                items.add(map(resultSet));
            }
            return items;
        } catch (SQLException e) {
            throw new RuntimeException("Could not load products", e);
        } finally {
            closeOwned(currentConnection);
        }
    }

    public Optional<LoanProduct> findById(int id) {
        Connection currentConnection = openConnection();
        try (PreparedStatement statement =
                currentConnection.prepareStatement("SELECT * FROM loan_products WHERE id=?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load product", e);
        } finally {
            closeOwned(currentConnection);
        }
    }

    /** Returns agreed submission terms; missing legacy terms require human reconciliation. */
    public LoanProduct termsForApplication(int applicationId) {
        Connection current = openConnection();
        try (PreparedStatement statement =
                current.prepareStatement(
                        "SELECT a.product_id AS id,t.* FROM application_terms t JOIN"
                                + " loan_applications a ON a.id=t.application_id WHERE"
                                + " t.application_id=?")) {
            statement.setInt(1, applicationId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("Original application terms need review");
                }
                return map(result);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load agreed terms", exception);
        } finally {
            closeOwned(current);
        }
    }

    public void update(LoanProduct product) {
        validate(product);
        String sql =
                "UPDATE loan_products SET"
                        + " name=?,interest_strategy=?,interest_rate=?,penalty_strategy=?,penalty_rate=?,duration_weeks=?,required_guarantees=?,weekly_savings=?"
                        + " WHERE id=?";
        Connection currentConnection = openConnection();
        try (PreparedStatement statement = currentConnection.prepareStatement(sql)) {
            set(statement, product);
            statement.setInt(9, product.id());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update product", e);
        } finally {
            closeOwned(currentConnection);
        }
    }

    public void delete(int id) {
        Connection currentConnection = openConnection();
        try (PreparedStatement statement =
                currentConnection.prepareStatement("DELETE FROM loan_products WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete a product used by applications", e);
        } finally {
            closeOwned(currentConnection);
        }
    }

    private void set(PreparedStatement statement, LoanProduct p) throws SQLException {
        statement.setString(1, p.name());
        statement.setString(2, p.interestStrategy());
        statement.setDouble(3, p.interestRate());
        statement.setString(4, p.penaltyStrategy());
        statement.setDouble(5, p.penaltyRate());
        statement.setInt(6, p.durationWeeks());
        statement.setInt(7, p.requiredGuarantees());
        statement.setDouble(8, p.weeklySavings());
    }

    private LoanProduct map(ResultSet resultSet) throws SQLException {
        return new LoanProduct(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("interest_strategy"),
                resultSet.getDouble("interest_rate"),
                resultSet.getString("penalty_strategy"),
                resultSet.getDouble("penalty_rate"),
                resultSet.getInt("duration_weeks"),
                resultSet.getInt("required_guarantees"),
                resultSet.getDouble("weekly_savings"));
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

    private void validate(LoanProduct p) {
        if (p == null || p.name() == null || p.name().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (!Double.isFinite(p.interestRate())
                || !Double.isFinite(p.penaltyRate())
                || !Double.isFinite(p.weeklySavings())
                || p.interestRate() < 0
                || p.penaltyRate() < 0
                || p.durationWeeks() <= 0
                || p.requiredGuarantees() < 0
                || p.weeklySavings() < 0) {
            throw new IllegalArgumentException(
                    "Product rates and savings cannot be negative, and duration must be positive");
        }
        if (!List.of("AGRICULTURAL", "BUSINESS", "EMERGENCY").contains(p.interestStrategy())) {
            throw new IllegalArgumentException("Unknown interest strategy");
        }
        if (!List.of("NORMAL", "GRACE").contains(p.penaltyStrategy())) {
            throw new IllegalArgumentException("Unknown penalty strategy");
        }
    }
}
