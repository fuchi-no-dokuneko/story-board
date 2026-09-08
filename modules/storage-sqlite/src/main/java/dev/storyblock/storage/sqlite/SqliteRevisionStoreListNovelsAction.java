package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

final class SqliteRevisionStoreListNovelsAction {
    static List<Ids.NovelId> listNovels(SqliteRevisionStore self)  {
        return self.read(connection -> {
            List<Ids.NovelId> novels = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT novel_id FROM novels ORDER BY novel_id"
            ); ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    novels.add(new Ids.NovelId(result.getString(1)));
                }
            }
            return List.copyOf(novels);
        });
    }
}
