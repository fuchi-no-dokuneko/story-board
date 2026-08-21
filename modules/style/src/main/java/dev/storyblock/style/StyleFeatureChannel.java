package dev.storyblock.style;

import java.util.EnumSet;
import java.util.Set;

public enum StyleFeatureChannel {
    SURFACE("surface", "surface-features-1.0.0", StyleDistanceMetric.JENSEN_SHANNON_DISTANCE, true),
    GRAMMAR("grammar", "grammar-features-1.0.0", StyleDistanceMetric.JENSEN_SHANNON_DISTANCE, true),
    RHYTHM("rhythm", "rhythm-features-1.0.0", StyleDistanceMetric.WASSERSTEIN_DISTANCE, true),
    NARRATIVE("narrative", "narrative-features-1.0.0", StyleDistanceMetric.ROBUST_L1_INPUT, true),
    LEXICAL("lexical", "lexical-features-1.0.0", StyleDistanceMetric.ROBUST_L1_INPUT, true),
    OPTIONAL_EMBEDDING(
            "optional_embedding",
            "content-reduced-embedding-1.0.0",
            StyleDistanceMetric.COSINE_DISTANCE,
            false
    );

    private final String canonicalName;
    private final String featureVersion;
    private final StyleDistanceMetric primaryMetric;
    private final boolean required;

    StyleFeatureChannel(
            String canonicalName,
            String featureVersion,
            StyleDistanceMetric primaryMetric,
            boolean required
    ) {
        this.canonicalName = canonicalName;
        this.featureVersion = featureVersion;
        this.primaryMetric = primaryMetric;
        this.required = required;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public String featureVersion() {
        return featureVersion;
    }

    public StyleDistanceMetric primaryMetric() {
        return primaryMetric;
    }

    public boolean required() {
        return required;
    }

    public static Set<StyleFeatureChannel> requiredChannels() {
        return Set.copyOf(EnumSet.of(SURFACE, GRAMMAR, RHYTHM, NARRATIVE, LEXICAL));
    }

    public static StyleFeatureChannel fromCanonicalName(String value) {
        for (StyleFeatureChannel channel : values()) {
            if (channel.canonicalName.equals(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unsupported style feature channel " + value);
    }
}
