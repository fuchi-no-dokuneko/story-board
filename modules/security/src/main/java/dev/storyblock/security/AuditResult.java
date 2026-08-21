package dev.storyblock.security;

public enum AuditResult {
    SUCCEEDED("succeeded"),
    IDEMPOTENT("idempotent"),
    REJECTED("rejected");

    private final String canonicalName;

    AuditResult(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static AuditResult fromCanonicalName(String value) {
        for (AuditResult result : values()) {
            if (result.canonicalName.equals(value)) {
                return result;
            }
        }
        throw new IllegalArgumentException("Unsupported audit result " + value);
    }
}
