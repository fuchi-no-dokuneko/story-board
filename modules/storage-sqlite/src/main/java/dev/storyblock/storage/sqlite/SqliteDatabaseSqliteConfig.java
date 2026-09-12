package dev.storyblock.storage.sqlite;

import org.sqlite.SQLiteConfig;

final class SqliteDatabaseSqliteConfig {
    static SQLiteConfig sqliteConfig(SqliteSettings settings, boolean explicitReadOnly) {
        SQLiteConfig sqlite = new SQLiteConfig();
        sqlite.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqlite.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
        sqlite.enforceForeignKeys(true);
        sqlite.setBusyTimeout(settings.busyTimeoutMillis());
        sqlite.setExplicitReadOnly(explicitReadOnly);
        sqlite.setTransactionMode(SQLiteConfig.TransactionMode.DEFERRED);
        sqlite.setSharedCache(false);
        sqlite.enableLoadExtension(false);
        return sqlite;
    }
}
