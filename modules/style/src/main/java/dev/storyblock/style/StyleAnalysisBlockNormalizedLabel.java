package dev.storyblock.style;



final class StyleAnalysisBlockNormalizedLabel {
    static String normalizedLabel(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Style analysis " + field + " is invalid");
        }
        return value.toLowerCase(java.util.Locale.ROOT);
    }
}
