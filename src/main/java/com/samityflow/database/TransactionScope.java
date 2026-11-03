package com.samityflow.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

/** Isolates a service operation without committing a caller's existing transaction. */
public final class TransactionScope implements AutoCloseable {
    private final Connection connection;
    private final boolean ownsTransaction;
    private final Savepoint savepoint;
    private boolean completed;

    public TransactionScope(Connection connection) throws SQLException {
        this.connection = connection;
        ownsTransaction = connection.getAutoCommit();
        if (ownsTransaction) {
            connection.setAutoCommit(false);
            savepoint = null;
        } else {
            savepoint = connection.setSavepoint();
        }
    }

    /** Commits only a transaction started by this scope; otherwise releases its savepoint. */
    public void complete() throws SQLException {
        if (ownsTransaction) {
            connection.commit();
        } else {
            connection.releaseSavepoint(savepoint);
        }
        completed = true;
    }

    @Override
    public void close() throws SQLException {
        try {
            if (!completed) {
                if (ownsTransaction) {
                    connection.rollback();
                } else {
                    connection.rollback(savepoint);
                    connection.releaseSavepoint(savepoint);
                }
            }
        } finally {
            if (ownsTransaction) {
                connection.setAutoCommit(true);
            }
        }
    }
}
