package dev.storyblock.style;



final class StyleCalibrationEngineHasChannel {
    static boolean hasChannel(
            StyleFeatureSet featureSet,
            StyleFeatureChannel channel
    ) {
        return featureSet.channels().stream().anyMatch(vector ->
                vector.channel() == channel
        );
    }
}
