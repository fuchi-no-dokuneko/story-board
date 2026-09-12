package dev.storyblock.storage.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

final class SqliteDatabaseRollback {
    static void rollback(Connection connection, Throwable primaryFailure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            primaryFailure.addSuppressed(rollbackFailure);
        }
    }
}
