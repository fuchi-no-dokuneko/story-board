package dev.storyblock.monitor;

public enum MonitorOutputKind {
    FINDING("finding"),
    PROPOSED_OPERATION("proposed_operation");

    private final String canonicalName;

    MonitorOutputKind(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static MonitorOutputKind fromCanonicalName(String value) {
        for (MonitorOutputKind kind : values()) {
            if (kind.canonicalName.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unsupported monitor output kind " + value);
    }
}
