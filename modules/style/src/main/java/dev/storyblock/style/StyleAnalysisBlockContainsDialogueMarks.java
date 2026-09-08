package dev.storyblock.style;



final class StyleAnalysisBlockContainsDialogueMarks {
    static boolean containsDialogueMarks(String text) {
        return text.indexOf('"') >= 0 || text.indexOf('\u201c') >= 0
                || text.indexOf('\u201d') >= 0 || text.indexOf('\u300c') >= 0
                || text.indexOf('\u300d') >= 0;
    }
}
