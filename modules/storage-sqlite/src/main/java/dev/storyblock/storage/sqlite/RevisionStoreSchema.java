package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.SQLiteConnection;

final class RevisionStoreSchema {
    private static final java.util.List<String> MIGRATIONS = java.util.List.of(
            "/db/migration/V001__immutable_revision_store.sql",
            "/db/migration/V002__canonical_transfer.sql",
            "/db/migration/V003__access_keys_and_audit.sql",
            "/db/migration/V004__monitor_runs.sql"
    );

    private RevisionStoreSchema() {
    }

    static void initialize(Connection connection) throws SQLException {
        for (String migration : MIGRATIONS) {
            String sql;
            try (InputStream input = RevisionStoreSchema.class.getResourceAsStream(migration)) {
                if (input == null) {
                    throw new StorageException("Missing revision store migration " + migration);
                }
                sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new StorageException("Could not read revision store migration", exception);
            }
            connection.unwrap(SQLiteConnection.class).getDatabase()._exec(sql);
        }
    }
}
