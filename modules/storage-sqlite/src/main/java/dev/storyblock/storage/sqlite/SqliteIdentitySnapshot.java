package dev.storyblock.storage.sqlite;

import java.sql.*;
import java.util.*;

/** Transaction-local ownership view; unchanged identities need no repeated SQL writes. */
final class SqliteIdentitySnapshot {
    private record Claim(String owner, String content) {}
    private final Connection connection;
    private final Map<String, Claim> known = new HashMap<>();
    private SqliteIdentitySnapshot(Connection connection) { this.connection = connection; }
    static SqliteIdentitySnapshot read(Connection connection, String novel) throws SQLException {
        var snapshot = new SqliteIdentitySnapshot(connection);
        try (var select = connection.prepareStatement("""
                SELECT identifier, owner, content_key FROM short_identifiers
                WHERE owner = ? OR identifier IN (
                    SELECT block_version_id FROM head_block_projection WHERE novel_id = ?)
                """)) {
            select.setString(1, novel); select.setString(2, novel);
            try (var rows = select.executeQuery()) {
                while (rows.next()) snapshot.known.put(rows.getString(1),
                        new Claim(rows.getString(2), rows.getString(3)));
            }
        }
        return snapshot;
    }
    void claim(String id, String owner, String content) throws SQLException {
        var expected = new Claim(owner, content);
        if (expected.equals(known.get(id))) return;
        SqliteIdentityClaim.claim(connection, id, owner, content);
        known.put(id, expected);
    }
}
