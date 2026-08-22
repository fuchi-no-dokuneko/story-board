package dev.storyblock.style;

public enum StyleAnalysisJobStatus {
    QUEUED("queued"),
    RUNNING("running"),
    SUCCEEDED("succeeded"),
    FAILED("failed");

    private final String canonicalName;

    StyleAnalysisJobStatus(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static StyleAnalysisJobStatus fromCanonicalName(String value) {
        for (StyleAnalysisJobStatus status : values()) {
            if (status.canonicalName.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported style analysis status " + value);
    }
}
