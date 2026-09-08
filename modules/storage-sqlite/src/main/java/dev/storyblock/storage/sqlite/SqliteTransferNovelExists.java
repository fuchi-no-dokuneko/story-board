package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.*;

final class SqliteTransferNovelExists {
    static boolean novelExists(Connection connection, Ids.NovelId novelId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM novels WHERE novel_id = ?"
        )) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
}
