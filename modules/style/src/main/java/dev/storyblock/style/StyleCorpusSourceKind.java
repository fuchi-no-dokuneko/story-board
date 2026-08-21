package dev.storyblock.style;

public enum StyleCorpusSourceKind {
    OWNER("owner"),
    LICENSED("licensed"),
    PUBLIC_DOMAIN("public_domain"),
    GENERATED("generated"),
    MIXED("mixed");

    private final String canonicalName;

    StyleCorpusSourceKind(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public boolean requiresExplicitGeneratedPromotion() {
        return this == GENERATED || this == MIXED;
    }

    public static StyleCorpusSourceKind fromCanonicalName(String value) {
        for (StyleCorpusSourceKind kind : values()) {
            if (kind.canonicalName.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unsupported corpus source kind " + value);
    }
}
