package dev.storyblock.storage.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteDataSource;

public final class SqliteDatabase implements AutoCloseable {
    private final HikariDataSource pool;
    private final SqliteMetrics metrics;
    private final Path databasePath;

    private SqliteDatabase(
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
        Objects.requireNonNull(databasePath, "databasePath");
        Objects.requireNonNull(settings, "settings");

        Path absolutePath = databasePath.toAbsolutePath().normalize();
        Path parent = absolutePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.isDirectory(absolutePath)) {
            throw new IOException("SQLite database path is a directory: " + absolutePath);
        }

        String jdbcUrl = "jdbc:sqlite:" + absolutePath;
        SQLiteDataSource migrationDataSource = new SQLiteDataSource(
                SqliteDatabaseSqliteConfig.sqliteConfig(settings, false)
        );
        migrationDataSource.setUrl(jdbcUrl);
        try {
            Flyway.configure()
                    .dataSource(migrationDataSource)
                    .locations("classpath:db/migration")
                    .validateMigrationNaming(true)
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load()
                    .migrate();
        } catch (RuntimeException exception) {
            throw new IOException("Could not migrate SQLite database " + absolutePath, exception);
        }

        SqliteMetrics metrics = new SqliteMetrics();
        VerifyingSqliteDataSource verified = new VerifyingSqliteDataSource(
                jdbcUrl,
                SqliteDatabaseSqliteConfig.sqliteConfig(settings, true),
                settings,
                metrics
        );

        HikariConfig hikari = new HikariConfig();
        hikari.setDataSource(verified);
        hikari.setPoolName("storyblock-sqlite-" + Integer.toUnsignedString(absolutePath.hashCode()));
        hikari.setMaximumPoolSize(settings.maximumPoolSize());
        hikari.setMinimumIdle(1);
        hikari.setAutoCommit(true);
        hikari.setReadOnly(false);
        hikari.setConnectionTimeout(Math.max(2_500L, settings.busyTimeoutMillis() + 1_000L));
        hikari.setValidationTimeout(1_000L);
        hikari.setInitializationFailTimeout(settings.busyTimeoutMillis() + 2_000L);
        hikari.setMaxLifetime(0L);
        return new SqliteDatabase(new HikariDataSource(hikari), metrics, absolutePath);
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
        long started = System.nanoTime();
        try (Connection connection = pool.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA wal_checkpoint(PASSIVE)")) {
            if (!result.next()) {
                throw new SQLException("wal_checkpoint returned no result");
            }
            SqliteWalCheckpoint checkpoint = new SqliteWalCheckpoint(
                    result.getInt(1),
                    result.getInt(2),
                    result.getInt(3),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            );
            metrics.recordCheckpoint(checkpoint.durationMillis());
            return checkpoint;
        } catch (SQLException exception) {
            metrics.recordFailure(exception);
            throw exception;
        }
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

    private <T> T transaction(boolean readOnly, SqliteWork<T> work) throws SQLException {
        Objects.requireNonNull(work, "work");
        long waitStarted = System.nanoTime();
        try (Connection connection = pool.getConnection()) {
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
                    metrics.recordRead(transactionNanos);
                } else {
                    metrics.recordWriteCommit(waitNanos, transactionNanos);
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
            metrics.recordFailure(exception);
            throw exception;
        }
    }

}
