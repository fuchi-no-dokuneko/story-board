package dev.storyblock.monitor;

import java.util.Objects;

public record MonitorSaveResult(StoredMonitorRun run, boolean idempotentReplay) {
    public MonitorSaveResult {
        Objects.requireNonNull(run, "run");
    }
}
