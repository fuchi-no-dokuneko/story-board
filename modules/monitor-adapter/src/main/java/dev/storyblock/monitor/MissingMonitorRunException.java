package dev.storyblock.monitor;

import dev.storyblock.domain.Ids;

public final class MissingMonitorRunException extends RuntimeException {
    public MissingMonitorRunException(
            Ids.NovelId novelId,
            Ids.MonitorRunId runId
    ) {
        super("Monitor run " + runId.value() + " does not exist for " + novelId.value());
    }
}
