package dev.storyblock.storage.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import org.sqlite.SQLiteConnection;

final class SqliteDatabaseTransactionAction {
    static <T> T transaction(SqliteDatabase self, boolean readOnly, SqliteWork<T> work) throws SQLException {
        Objects.requireNonNull(work, "work");
        long waitStarted = System.nanoTime();
        try (Connection connection = self.pool.getConnection()) {
            long waitNanos = System.nanoTime() - waitStarted;
            connection.unwrap(SQLiteConnection.class).setFirstStatementExecuted(false);
            connection.setReadOnly(readOnly);
            connection.setAutoCommit(false);
            SqliteDatabasePrepareTransaction.prepareTransaction(connection, readOnly);
            long transactionStarted = System.nanoTime();
            Throwable primaryFailure = null;
            try {
                T result = work.execute(connection);
                connection.commit();
                long transactionNanos = System.nanoTime() - transactionStarted;
                if (readOnly) {
                    self.metrics.recordRead(transactionNanos);
                } else {
                    self.metrics.recordWriteCommit(waitNanos, transactionNanos);
                }
                return result;
            } catch (SQLException | RuntimeException | Error exception) {
                primaryFailure = exception;
                SqliteDatabaseRollback.rollback(connection, exception);
                throw exception;
            } finally {
                SqliteDatabaseReset.reset(connection, readOnly, primaryFailure);
            }
        } catch (SQLException exception) {
            self.metrics.recordFailure(exception);
            throw exception;
        }
    }
}
