package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import dev.storyblock.security.*;
import dev.storyblock.monitor.*;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import java.util.Objects;

interface SqliteMonitorPort extends MonitorStore, SqliteStoreContext {
  @Override
  default MonitorSaveResult saveMonitorRun(StoredMonitorRun run) {
    Objects.requireNonNull(run, "run");
    return context().write(connection -> SqliteMonitorSave.save(connection, run));
  }

  @Override
  default StoredMonitorRun getMonitorRun(
      Ids.NovelId novelId,
      Ids.MonitorRunId runId
  ) {
    Objects.requireNonNull(novelId, "novelId");
    Objects.requireNonNull(runId, "runId");
    return context().read(connection -> SqliteMonitorGet.get(connection, novelId, runId));
  }
}
