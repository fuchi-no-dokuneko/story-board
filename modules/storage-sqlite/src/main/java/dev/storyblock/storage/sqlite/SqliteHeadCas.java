package dev.storyblock.storage.sqlite;
import dev.storyblock.contracts.*;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteHeadCas {
  static void update(Connection connection, CommitRequest request, Ids.NovelId novelId, long sequence) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE novels
                SET head_revision_id = ?, head_sequence = ?, head_hash = ?
                WHERE novel_id = ? AND head_revision_id = ? AND head_hash = ?
                """)) {
      statement.setString(1, request.candidate().id().value());
      statement.setLong(2, sequence);
      statement.setString(3, request.candidateHash());
      statement.setString(4, novelId.value());
      statement.setString(5, request.expectedHead().revisionId().value());
      statement.setString(6, request.expectedHead().contentHash());
      if (statement.executeUpdate() != 1) {
        throw new StaleHeadException(
            request.expectedHead(), SqliteRevisionStoreRequireHead.requireHead(connection, novelId)
        );
      }
    }
  }
}
