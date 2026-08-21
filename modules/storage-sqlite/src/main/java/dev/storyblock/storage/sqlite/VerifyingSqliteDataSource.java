package dev.storyblock.storage.sqlite;

import java.sql.SQLException;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteDataSource;

final class VerifyingSqliteDataSource extends SQLiteDataSource {
    private final SqliteSettings settings;
    private final SqliteMetrics metrics;

    VerifyingSqliteDataSource(
            String jdbcUrl,
            SQLiteConfig config,
            SqliteSettings settings,
            SqliteMetrics metrics
    ) {
        super(config);
        setUrl(jdbcUrl);
        this.settings = settings;
        this.metrics = metrics;
    }

    @Override
    public SQLiteConnection getConnection(String username, String password) throws SQLException {
        return verify(super.getConnection(username, password));
    }

    private SQLiteConnection verify(SQLiteConnection connection) throws SQLException {
        try {
            SqlitePragmas.read(connection).require(settings);
            metrics.recordConnectionVerification();
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }
}
