package dev.storyblock.style;



final class StyleWindowRequireLabel {
    static String requireLabel(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Style window " + field + " is invalid");
        }
        return value;
    }
}
