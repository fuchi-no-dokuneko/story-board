package dev.storyblock.style;

public enum StyleWindowKind {
    OPERATIONAL("operational"),
    MICRO("micro"),
    NON_OVERLAP("non_overlap");

    private final String canonicalName;

    StyleWindowKind(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public boolean primaryDecisionWindow() {
        return this == OPERATIONAL;
    }

    public boolean localizationOnly() {
        return this == MICRO;
    }

    public boolean sustainmentEvidence() {
        return this == NON_OVERLAP;
    }

    public static StyleWindowKind fromCanonicalName(String value) {
        for (StyleWindowKind kind : values()) {
            if (kind.canonicalName.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unsupported style window kind " + value);
    }
}
