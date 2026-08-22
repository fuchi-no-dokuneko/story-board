package dev.storyblock.style;

public record StyleWindowFeatures(
        StyleWindow window,
        StyleFeatureSet featureSet
) {
    public StyleWindowFeatures {
        java.util.Objects.requireNonNull(window, "window");
        java.util.Objects.requireNonNull(featureSet, "featureSet");
    }
}
