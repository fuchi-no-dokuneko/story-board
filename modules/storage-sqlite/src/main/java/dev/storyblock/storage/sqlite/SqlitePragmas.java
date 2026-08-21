package dev.storyblock.storage.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import org.sqlite.SQLiteConnection;

public record SqlitePragmas(
        String journalMode,
        int synchronous,
        boolean foreignKeys,
        int busyTimeoutMillis,
        boolean queryOnly,
        boolean explicitReadOnly,
        boolean loadExtensionsEnabled
) {
    private static final int SQLITE_SYNCHRONOUS_FULL = 2;

    public static SqlitePragmas read(Connection connection) throws SQLException {
        SQLiteConnection sqliteConnection = connection.unwrap(SQLiteConnection.class);
        boolean firstStatementExecuted = sqliteConnection.isFirstStatementExecuted();
        try {
            return new SqlitePragmas(
                    textPragma(connection, "journal_mode").toLowerCase(Locale.ROOT),
                    intPragma(connection, "synchronous"),
                    intPragma(connection, "foreign_keys") == 1,
                    intPragma(connection, "busy_timeout"),
                    intPragma(connection, "query_only") == 1,
                    sqliteConnection.getDatabase().getConfig().isExplicitReadOnly(),
                    sqliteConnection.getDatabase().getConfig().isEnabledLoadExtension()
            );
        } finally {
            sqliteConnection.setFirstStatementExecuted(firstStatementExecuted);
        }
    }

    public void require(SqliteSettings settings) throws SQLException {
        if (!journalMode.equals("wal")) {
            throw new SQLException("Expected journal_mode=WAL but found " + journalMode);
        }
        if (synchronous != SQLITE_SYNCHRONOUS_FULL) {
            throw new SQLException("Expected synchronous=FULL but found " + synchronous);
        }
        if (!foreignKeys) {
            throw new SQLException("Expected foreign_keys=ON");
        }
        if (busyTimeoutMillis != settings.busyTimeoutMillis()) {
            throw new SQLException("Expected busy_timeout=" + settings.busyTimeoutMillis()
                    + " but found " + busyTimeoutMillis);
        }
        if (queryOnly) {
            throw new SQLException("New pooled connection unexpectedly has query_only enabled");
        }
        if (!explicitReadOnly) {
            throw new SQLException("Xerial explicit read-only mode is disabled");
        }
        if (loadExtensionsEnabled) {
            throw new SQLException("SQLite loadable extensions must remain disabled");
        }
    }

    private static String textPragma(Connection connection, String name) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA " + name)) {
            if (!result.next()) {
                throw new SQLException("PRAGMA " + name + " returned no row");
            }
            return result.getString(1);
        }
    }

    private static int intPragma(Connection connection, String name) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA " + name)) {
            if (!result.next()) {
                throw new SQLException("PRAGMA " + name + " returned no row");
            }
            return result.getInt(1);
        }
    }
}
