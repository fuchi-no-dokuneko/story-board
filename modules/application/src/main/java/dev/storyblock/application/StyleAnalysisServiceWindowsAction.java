package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisWindowPage;
import dev.storyblock.style.StyleAnalysisWindowSlice;

final class StyleAnalysisServiceWindowsAction {
    static StyleAnalysisWindowPage windows(StyleAnalysisService self, Ids.StyleAnalysisId analysisId, String cursor, int limit)  {
        int after = cursor == null ? -1 : StyleAnalysisServiceDecodeCursor.decodeCursor(analysisId, cursor);
        StyleAnalysisWindowSlice slice = self.analyses.listStyleAnalysisWindows(
                analysisId, after, limit
        );
        String next = slice.nextOrdinal() == null
                ? null : StyleAnalysisServiceEncodeCursor.encodeCursor(analysisId, slice.nextOrdinal());
        return new StyleAnalysisWindowPage(analysisId, slice.items(), next);
    }
}
