package dev.storyblock.monitor;

public enum MonitorRunState {
    CURRENT("current"),
    STALE("stale");

    private final String canonicalName;

    MonitorRunState(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
