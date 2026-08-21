package dev.storyblock.style;

public enum StyleScopeKind {
    NOVEL("novel"),
    SERIES("series"),
    CHARACTER("character");

    private final String canonicalName;

    StyleScopeKind(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static StyleScopeKind fromCanonicalName(String value) {
        for (StyleScopeKind kind : values()) {
            if (kind.canonicalName.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unsupported style scope kind " + value);
    }
}
