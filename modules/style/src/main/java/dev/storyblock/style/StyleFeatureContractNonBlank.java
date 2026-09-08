package dev.storyblock.style;



final class StyleFeatureContractNonBlank {
    static String nonBlank(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Style " + field + " is invalid");
        }
        return value;
    }
}
