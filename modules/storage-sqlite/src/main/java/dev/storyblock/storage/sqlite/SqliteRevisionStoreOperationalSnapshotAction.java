package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StorageException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class SqliteRevisionStoreOperationalSnapshotAction {
  static SqliteOperationalSnapshot operationalSnapshot(SqliteRevisionStore self, Instant now)  {
    Objects.requireNonNull(now, "now");
    SqliteMetrics.Snapshot metrics = self.database.metrics();
    long walBytes;
    try {
      walBytes = self.database.walBytes();
    } catch (IOException exception) {
      throw new StorageException("Could not inspect the SQLite WAL", exception);
    }
    long capturedWalBytes = walBytes;
    return self.read(connection -> {
      long[] values = new long[5];
      String migrationVersion;
      try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT
                      (SELECT COUNT(*) FROM analysis_jobs WHERE status = 'queued'),
                      CAST(COALESCE((julianday(?) - julianday(
                        (SELECT MIN(created_at) FROM analysis_jobs WHERE status = 'queued')
                      )) * 86400, 0) AS INTEGER),
                      CAST(COALESCE((SELECT AVG(
                        (julianday(r.completed_at) - julianday(j.created_at)) * 86400000
                      ) FROM analysis_runs r JOIN analysis_jobs j ON j.job_id = r.job_id), 0)
                        AS INTEGER),
                      (SELECT COUNT(*) FROM rewrite_proposals WHERE state = 'stale'),
                      (SELECT COALESCE(SUM(size_bytes), 0) FROM artifacts)
                    """)) {
        statement.setString(1, now.toString());
        try (ResultSet result = statement.executeQuery()) {
          result.next();
          for (int index = 0; index < values.length; index++) {
            values[index] = result.getLong(index + 1);
          }
        }
      }
      try (var statement = connection.createStatement();
        var result = statement.executeQuery("""
                         SELECT version FROM flyway_schema_history
                         WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1
                         """)) {
        migrationVersion = result.next() ? result.getString(1) : "none";
      }
      Map<String, Long> findings = new LinkedHashMap<>();
      try (var statement = connection.createStatement();
        var result = statement.executeQuery("""
                         SELECT code, COUNT(*) AS total FROM issues GROUP BY code ORDER BY code
                         """)) {
        while (result.next()) {
          findings.put(result.getString("code"), result.getLong("total"));
        }
      }
      return new SqliteOperationalSnapshot(
          metrics.writerWaitMillis(),
          metrics.maxTransactionMillis(),
          metrics.sqliteBusyTotal(),
          capturedWalBytes,
          metrics.lastCheckpointMillis(),
          values[0],
          values[1],
          values[2],
          0L,
          values[3],
          values[4],
          migrationVersion,
          findings
      );
    });
  }
}
