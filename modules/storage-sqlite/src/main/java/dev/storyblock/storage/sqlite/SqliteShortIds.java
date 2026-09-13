package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.ShortIdScope;
import dev.storyblock.domain.ShortIds;
import java.sql.*;

public final class SqliteShortIds implements ShortIdScope.Allocator {
    private final SqliteRevisionStore store;
    public SqliteShortIds(SqliteRevisionStore store) { this.store = store; }

    public String allocate(String prefix, String key) {
        return store.write(connection -> {
            try (var select = connection.prepareStatement(
                    "SELECT identifier FROM short_identifiers WHERE allocation_key = ?")) {
                select.setString(1, key);
                try (var rows = select.executeQuery()) {
                    if (rows.next()) return ShortIds.require(rows.getString(1), prefix);
                }
            }
            for (int attempt = 0; ; attempt++) {
                String id = ShortIds.candidate(prefix, key, attempt);
                try (var insert = connection.prepareStatement(
                        "INSERT OR IGNORE INTO short_identifiers(identifier, allocation_key) VALUES (?, ?)")) {
                    insert.setString(1, id); insert.setString(2, key);
                    if (insert.executeUpdate() == 1) return id;
                }
            }
        });
    }
}
