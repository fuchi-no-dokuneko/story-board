package dev.storyblock.style;

public enum StyleCalibrationConfidence {
    CALIBRATED("calibrated"),
    LOW_CONFIDENCE("low_confidence");

    private final String canonicalName;

    StyleCalibrationConfidence(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static StyleCalibrationConfidence fromCanonicalName(String value) {
        for (StyleCalibrationConfidence confidence : values()) {
            if (confidence.canonicalName.equals(value)) {
                return confidence;
            }
        }
        throw new IllegalArgumentException("Unsupported style confidence " + value);
    }
}
