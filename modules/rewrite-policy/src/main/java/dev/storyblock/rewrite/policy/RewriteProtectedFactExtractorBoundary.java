package dev.storyblock.rewrite.policy;



final class RewriteProtectedFactExtractorBoundary {
    static boolean boundary(String value, int index) {
        return index < 0 || index >= value.length()
                || !Character.isLetterOrDigit(value.codePointAt(index));
    }
}
