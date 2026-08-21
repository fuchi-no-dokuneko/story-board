package dev.storyblock.style;

public enum StyleProfileState {
    DRAFT("draft"),
    CALIBRATING("calibrating"),
    READY("ready"),
    DEPRECATED("deprecated");

    private final String canonicalName;

    StyleProfileState(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public boolean canTransitionTo(StyleProfileState target) {
        return switch (this) {
            case DRAFT -> target == CALIBRATING;
            case CALIBRATING -> target == READY;
            case READY -> target == DEPRECATED;
            case DEPRECATED -> false;
        };
    }

    public static StyleProfileState fromCanonicalName(String value) {
        for (StyleProfileState state : values()) {
            if (state.canonicalName.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported style profile state " + value);
    }
}
