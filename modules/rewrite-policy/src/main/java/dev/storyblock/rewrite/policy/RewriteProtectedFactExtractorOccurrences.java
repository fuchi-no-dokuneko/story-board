package dev.storyblock.rewrite.policy;



final class RewriteProtectedFactExtractorOccurrences {
    static int occurrences(String text, String value, boolean wordBoundary) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(value, from)) >= 0) {
            int end = from + value.length();
            if (!wordBoundary || (RewriteProtectedFactExtractorBoundary.boundary(text, from - 1) && RewriteProtectedFactExtractorBoundary.boundary(text, end))) {
                count++;
            }
            from = end;
        }
        return count;
    }
}
