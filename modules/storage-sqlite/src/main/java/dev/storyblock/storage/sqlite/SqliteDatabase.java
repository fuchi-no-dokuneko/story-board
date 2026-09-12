package dev.storyblock.storage.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public final class SqliteDatabase implements AutoCloseable {
    final HikariDataSource pool;
    final SqliteMetrics metrics;
    final Path databasePath;

    SqliteDatabase(
            HikariDataSource pool,
            SqliteMetrics metrics,
            Path databasePath
    ) {
        this.pool = pool;
        this.metrics = metrics;
        this.databasePath = databasePath;
    }

    public static SqliteDatabase open(Path databasePath) throws IOException {
        return open(databasePath, SqliteSettings.DEFAULT);
    }

    static SqliteDatabase open(Path databasePath, SqliteSettings settings) throws IOException {
        return SqliteDatabaseOpenFactory.open(databasePath, settings);
    }

    public <T> T readOnly(SqliteWork<T> work) throws SQLException {
        return transaction(true, work);
    }

    public <T> T write(SqliteWork<T> work) throws SQLException {
        metrics.recordWriteAttempt();
        return transaction(false, work);
    }

    public SqlitePragmas inspectPragmas() throws SQLException {
        try (Connection connection = pool.getConnection()) {
            return SqlitePragmas.read(connection);
        } catch (SQLException exception) {
            metrics.recordFailure(exception);
            throw exception;
        }
    }

    public SqliteWalCheckpoint checkpointPassive() throws SQLException {
        return SqliteDatabaseCheckpointPassiveAction.checkpointPassive(this);
    }

    public SqliteMetrics.Snapshot metrics() {
        return metrics.snapshot();
    }

    long walBytes() throws IOException {
        Path wal = Path.of(databasePath + "-wal");
        return Files.exists(wal) ? Files.size(wal) : 0L;
    }

    Connection borrowConnection() throws SQLException {
        return pool.getConnection();
    }

    @Override
    public void close() {
        pool.close();
    }

    <T> T transaction(boolean readOnly, SqliteWork<T> work) throws SQLException {
        return SqliteDatabaseTransactionAction.transaction(this, readOnly, work);
    }

}
