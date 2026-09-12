package dev.storyblock.application;



final class AgentNovelRegistrationServiceRequireText {
    static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(
                    field + " must contain 1 to " + maxLength + " Unicode characters"
            );
        }
    }
}
