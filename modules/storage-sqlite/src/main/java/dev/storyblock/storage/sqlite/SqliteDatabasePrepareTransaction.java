package dev.storyblock.storage.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;

final class SqliteDatabasePrepareTransaction {
    static void prepareTransaction(Connection connection, boolean readOnly)
            throws SQLException {
        SQLiteConnection sqlite = connection.unwrap(SQLiteConnection.class);
        sqlite.setFirstStatementExecuted(false);
        if (readOnly) {
            sqlite.getDatabase()._exec("PRAGMA query_only = true;");
            return;
        }

        sqlite.getDatabase()._exec("commit;");
        sqlite.getDatabase()._exec("PRAGMA query_only = false;");
        sqlite.getDatabase()._exec("BEGIN IMMEDIATE;");
        sqlite.setCurrentTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
    }
}
