package dev.storyblock.detector;

public enum FindingCode {
    LOCATION_CHANGED_WITHOUT_TRANSITION(FindingSeverity.WARNING),
    CHARACTER_APPEARED_WITHOUT_ENTER(FindingSeverity.ERROR),
    CHARACTER_DISAPPEARED_WITHOUT_EXIT(FindingSeverity.ERROR),
    WEATHER_CHANGED_WITHOUT_EVIDENCE(FindingSeverity.WARNING),
    TIME_DISCONTINUITY(FindingSeverity.WARNING),
    POV_CHANGED_WITHOUT_BOUNDARY(FindingSeverity.WARNING),
    META_TEXT_MISMATCH(FindingSeverity.ERROR),
    INTENTIONAL_SCENE_RESET(FindingSeverity.INFO);

    private final FindingSeverity defaultSeverity;

    FindingCode(FindingSeverity defaultSeverity) {
        this.defaultSeverity = defaultSeverity;
    }

    public FindingSeverity defaultSeverity() {
        return defaultSeverity;
    }
}
