package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.storage.StorageException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;

public final class SqliteRevisionStore implements SqliteRevisionReadPort,
    SqliteRevisionWritePort, SqliteTransferPort, SqliteAccessKeyPort,
    SqliteMonitorPort, SqliteProfilePort, SqliteAnalysisPort,
    SqliteRewritePort, SqliteOperationsPort {
  final SqliteDatabase database;
  final CheckpointPolicy checkpointPolicy;
  final CommitFaultInjector faultInjector;
  final ImportFaultInjector importFaultInjector;

  SqliteRevisionStore(SqliteDatabase database, CheckpointPolicy checkpointPolicy,
      CommitFaultInjector faultInjector, ImportFaultInjector importFaultInjector) {
    this.database = Objects.requireNonNull(database, "database");
    this.checkpointPolicy = Objects.requireNonNull(checkpointPolicy, "checkpointPolicy");
    this.faultInjector = Objects.requireNonNull(faultInjector, "faultInjector");
    this.importFaultInjector = Objects.requireNonNull(importFaultInjector, "importFaultInjector");
    write(connection -> null);
  }

  public SqliteRevisionStore context() { return this; }

  public static SqliteRevisionStore open(Path path) throws IOException {
    return open(path, CheckpointPolicy.DEFAULT);
  }

  public static SqliteRevisionStore open(Path path, CheckpointPolicy policy) throws IOException {
    return open(path, policy, CommitFaultInjector.NONE);
  }

  static SqliteRevisionStore open(Path path, CheckpointPolicy policy,
      CommitFaultInjector fault) throws IOException {
    return open(path, policy, fault, ImportFaultInjector.NONE);
  }

  static SqliteRevisionStore open(Path path, CheckpointPolicy policy,
      CommitFaultInjector fault, ImportFaultInjector importFault) throws IOException {
    return new SqliteRevisionStore(SqliteDatabase.open(path), policy, fault, importFault);
  }

  <T> T read(SqliteWork<T> work) {
    try { return database.readOnly(work); }
    catch (SQLException exception) { throw new StorageException("SQLite revision read failed", exception); }
  }

  <T> T write(SqliteWork<T> work) {
    try { return database.write(work); }
    catch (SQLException exception) { throw new StorageException("SQLite revision write failed", exception); }
  }

  record LocatedBlock(Ids.SceneId sceneId, NarrativeBlock block) {}
}
