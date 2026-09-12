package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import dev.storyblock.security.*;
import dev.storyblock.monitor.*;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import java.sql.SQLException;
import java.time.Instant;

interface SqliteOperationsPort extends AutoCloseable, SqliteStoreContext {
  default SqliteOperationalSnapshot operationalSnapshot(Instant now) {
    return SqliteRevisionStoreOperationalSnapshotAction.operationalSnapshot(context(), now);
  }

  default void verifyReadableAndWritable() {
    context().write(connection -> {
      try (var statement = connection.createStatement();
        var result = statement.executeQuery("SELECT 1")) {
        if (!result.next() || result.getInt(1) != 1) {
          throw new SQLException("SQLite health query returned an invalid result");
        }
      }
      return null;
    });
  }

  default SqliteWalCheckpoint checkpointPassive() {
    try {
      return context().database.checkpointPassive();
    } catch (SQLException exception) {
      throw new StorageException("Could not inspect the SQLite checkpoint", exception);
    }
  }

  @Override
  default void close() {
    context().database.close();
  }
}
