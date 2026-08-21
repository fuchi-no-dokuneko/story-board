package dev.storyblock.style;

public enum StyleDistanceMetric {
    JENSEN_SHANNON_DISTANCE("jensen_shannon_distance"),
    WASSERSTEIN_DISTANCE("wasserstein_distance"),
    ROBUST_L1_INPUT("robust_l1_input"),
    COSINE_DISTANCE("cosine_distance");

    private final String canonicalName;

    StyleDistanceMetric(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static StyleDistanceMetric fromCanonicalName(String value) {
        for (StyleDistanceMetric metric : values()) {
            if (metric.canonicalName.equals(value)) {
                return metric;
            }
        }
        throw new IllegalArgumentException("Unsupported style distance metric " + value);
    }
}
