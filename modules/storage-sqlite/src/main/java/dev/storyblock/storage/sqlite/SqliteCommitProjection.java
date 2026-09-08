package dev.storyblock.storage.sqlite;
import dev.storyblock.contracts.*;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.*;
import java.sql.Connection;
import java.sql.SQLException;

final class SqliteCommitProjection {
  static void update(SqliteRevisionStore self, Connection connection, CommitRequest request, Ids.NovelId novelId, long sequence, byte[] candidateBytes) throws SQLException {
    SqliteRevisionStoreInsertTombstonesAction.insertTombstones(self, connection, request);
    self.faultInjector.after(CommitStage.AFTER_TOMBSTONES);
    SqliteRevisionStoreRebuildProjection.rebuildProjection(connection, request.candidate());
    self.faultInjector.after(CommitStage.AFTER_PROJECTION);
    if (SqliteRevisionStoreShouldCheckpointAction.shouldCheckpoint(self, connection, novelId, sequence)) {
      SqliteRevisionStoreInsertCheckpoint.insertCheckpoint(
          connection,
          request.candidate(),
          sequence,
          request.candidateHash(),
          candidateBytes
      );
    }
    self.faultInjector.after(CommitStage.AFTER_CHECKPOINT);
  }
}
