package dev.storyblock.storage.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteDataSource;

final class SqliteDatabaseOpenFactory {
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
}
