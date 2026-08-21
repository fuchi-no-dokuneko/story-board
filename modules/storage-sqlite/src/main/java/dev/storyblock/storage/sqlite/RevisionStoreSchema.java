package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.SQLiteConnection;

final class RevisionStoreSchema {
    private static final String MIGRATION = "/db/migration/V001__immutable_revision_store.sql";

    private RevisionStoreSchema() {
    }

    static void initialize(Connection connection) throws SQLException {
        String sql;
        try (InputStream input = RevisionStoreSchema.class.getResourceAsStream(MIGRATION)) {
            if (input == null) {
                throw new StorageException("Missing revision store migration " + MIGRATION);
            }
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new StorageException("Could not read revision store migration", exception);
        }
        connection.unwrap(SQLiteConnection.class).getDatabase()._exec(sql);
    }
}
