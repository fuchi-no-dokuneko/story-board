package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

final class SqliteRevisionStoreCountAction {
    static long count(SqliteRevisionStore self, Ids.NovelId novelId, String table)  {
        if (!table.equals("revisions") && !table.equals("operations")) {
            throw new IllegalArgumentException("Unsupported count table");
        }
        return self.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM " + table + " WHERE novel_id = ?"
            )) {
                statement.setString(1, novelId.value());
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return result.getLong(1);
                }
            }
        });
    }
}
