package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.ShortIdScope;
import dev.storyblock.storage.IdentityConflictException;
import java.sql.*;
import java.util.Objects;

final class SqliteIdentityClaim {
    static void claim(Connection connection, String id, String owner, String content) throws SQLException {
        try (var insert = connection.prepareStatement(
                "INSERT OR IGNORE INTO short_identifiers(identifier, owner, content_key) VALUES (?, ?, ?)")) {
            insert.setString(1, id); insert.setString(2, owner); insert.setString(3, content);
            if (insert.executeUpdate() == 1) return;
        }
        try (var select = connection.prepareStatement(
                "SELECT owner, content_key FROM short_identifiers WHERE identifier = ?")) {
            select.setString(1, id);
            try (var rows = select.executeQuery()) {
                if (!rows.next()) throw new IdentityConflictException(id);
                String priorOwner = rows.getString(1);
                if (priorOwner != null) {
                    if (!owner.equals(priorOwner) || !Objects.equals(content, rows.getString(2)))
                        throw new IdentityConflictException(id);
                    return;
                }
                if (!ShortIdScope.owns(id)) throw new IdentityConflictException(id);
            }
        }
        try (var update = connection.prepareStatement(
                "UPDATE short_identifiers SET owner = ?, content_key = ? WHERE identifier = ?")) {
            update.setString(1, owner); update.setString(2, content); update.setString(3, id);
            update.executeUpdate();
        }
    }
}
