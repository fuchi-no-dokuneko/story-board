package dev.storyblock.style;



final class StyleFeatureAnalyzerBucket {
    static String bucket(String prefix, int value, int width) {
        int lower = Math.max(0, value / width * width);
        return prefix + ":" + lower;
    }
}
