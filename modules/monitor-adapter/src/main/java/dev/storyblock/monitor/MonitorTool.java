package dev.storyblock.monitor;

public enum MonitorTool {
    SUBMIT_FINDING("submit_finding"),
    SUBMIT_PROPOSED_OPERATION("submit_proposed_operation");

    private final String canonicalName;

    MonitorTool(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
