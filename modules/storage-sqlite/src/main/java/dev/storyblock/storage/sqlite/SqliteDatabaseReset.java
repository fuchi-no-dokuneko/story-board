package dev.storyblock.storage.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class SqliteDatabaseReset {
    static void reset(
            Connection connection,
            boolean readOnly,
            Throwable primaryFailure
    ) throws SQLException {
        SQLException resetFailure = null;
        try {
            if (!connection.getAutoCommit()) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            resetFailure = exception;
        }
        if (readOnly) {
            try {
                connection.setReadOnly(false);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA query_only = false");
                }
            } catch (SQLException exception) {
                if (resetFailure == null) {
                    resetFailure = exception;
                } else {
                    resetFailure.addSuppressed(exception);
                }
            }
        }
        if (resetFailure != null) {
            if (primaryFailure != null) {
                primaryFailure.addSuppressed(resetFailure);
            } else {
                throw resetFailure;
            }
        }
    }
}
