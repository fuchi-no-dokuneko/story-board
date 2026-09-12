package dev.storyblock.renderer;



final class MetadataResolutionStateRequiredString {
    static String requiredString(Object value, String label) {
        if (!(value instanceof String string) || string.isBlank()) {
            throw new IllegalArgumentException(label + " must be a non-blank string");
        }
        return string;
    }
}
