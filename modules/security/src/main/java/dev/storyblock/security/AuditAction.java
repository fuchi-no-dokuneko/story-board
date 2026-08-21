package dev.storyblock.security;

public enum AuditAction {
    ACCESS_KEY_ISSUE("access-key-issue"),
    ACCESS_KEY_REVOKE("access-key-revoke"),
    CANONICAL_IMPORT("canonical-import"),
    CANONICAL_EXPORT("canonical-export"),
    COMMIT("commit");

    private final String canonicalName;

    AuditAction(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static AuditAction fromCanonicalName(String value) {
        for (AuditAction action : values()) {
            if (action.canonicalName.equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unsupported audit action " + value);
    }
}
