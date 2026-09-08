package dev.storyblock.storage.sqlite;
import dev.storyblock.style.StyleProfileVersionSaveResult;
import java.sql.*;
import static dev.storyblock.storage.sqlite.SqliteProfileData.*;
final class SqliteProfileReplay {
    static StyleProfileVersionSaveResult replay(Connection connection, Mutation prior,
            MutationKind kind, String requestHash, String key) throws SQLException {
        Mutation receipt = SqliteProfileRequireReplay.requireReplay(prior, kind, requestHash, key);
        return new StyleProfileVersionSaveResult(SqliteProfileGetVersion.getVersion(connection,
                receipt.profileId(), SqliteProfileRequireVersionId.requireVersionId(receipt)), true);
    }
}
