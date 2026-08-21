package dev.storyblock.monitor;

public enum MonitorStaleReason {
    HEAD_CHANGED("head_changed"),
    AFFECTED_BLOCK_CHANGED("affected_block_changed"),
    AFFECTED_BLOCK_MISSING("affected_block_missing"),
    RULE_VERSION_CHANGED("rule_version_changed");

    private final String canonicalName;

    MonitorStaleReason(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
