package dev.storyblock.monitor;

import dev.storyblock.domain.Ids;

public interface MonitorStore {
    MonitorSaveResult saveMonitorRun(StoredMonitorRun run);

    StoredMonitorRun getMonitorRun(
            Ids.NovelId novelId,
            Ids.MonitorRunId runId
    );
}
