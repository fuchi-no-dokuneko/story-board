package dev.storyblock.style;



final class StyleFeatureAnalyzerBucketStart {
    static int bucketStart(String key) {
        int colon = key.lastIndexOf(':');
        if (colon < 0) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(key.substring(colon + 1));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }
}
