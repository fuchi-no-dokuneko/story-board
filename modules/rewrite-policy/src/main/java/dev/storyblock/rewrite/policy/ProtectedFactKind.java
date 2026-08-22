package dev.storyblock.rewrite.policy;

public enum ProtectedFactKind {
    NAME("name", RewriteFactDisposition.BLOCK),
    PLACE("place", RewriteFactDisposition.BLOCK),
    NUMBER("number", RewriteFactDisposition.BLOCK),
    NEGATION("negation", RewriteFactDisposition.BLOCK),
    CAUSALITY("causality", RewriteFactDisposition.MANUAL_ONLY),
    SPEAKER("speaker", RewriteFactDisposition.MANUAL_ONLY),
    PRESENCE("presence", RewriteFactDisposition.MANUAL_ONLY),
    EVIDENCE("evidence", RewriteFactDisposition.BLOCK),
    HIGH_RISK_METADATA("high_risk_metadata", RewriteFactDisposition.MANUAL_ONLY);

    private final String canonicalName;
    private final RewriteFactDisposition changedDisposition;

    ProtectedFactKind(
            String canonicalName,
            RewriteFactDisposition changedDisposition
    ) {
        this.canonicalName = canonicalName;
        this.changedDisposition = changedDisposition;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public RewriteFactDisposition changedDisposition() {
        return changedDisposition;
    }
}
