package dev.storyblock.application;

import dev.storyblock.domain.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenStylePipelineTest {
    @Test void styleFeaturesHashesWindowsAndCatalogIgnoreNotes() {
        var full = HiddenBlockFixture.revision(); var plain = HiddenBlockFixture.withoutHidden(full);
        var lexicon = StyleMaskingLexicon.empty();
        var contract = StyleFeatureContract.defaults(lexicon.vocabularyHash());
        var analyzer = new StyleFeatureAnalyzer();
        assertEquals(analyzer.extract(plain.liveBlocks(),lexicon,contract),
                analyzer.extract(full.liveBlocks(),lexicon,contract));
        assertEquals(StyleFeatureAnalyzer.sourceHash(plain.liveBlocks()),
                StyleFeatureAnalyzer.sourceHash(full.liveBlocks()));
        var config = StyleWindowConfiguration.defaults();
        var planner = new StyleWindowPlanner();
        assertEquals(planner.plan(plain,config),planner.plan(full,config));
        var rolling = new StyleRollingWindowAnalyzer();
        assertEquals(rolling.analyze(plain,lexicon,contract,config,java.util.Map.of()),
                rolling.analyze(full,lexicon,contract,config,java.util.Map.of()));
        var head = new RevisionRef(full.id(),0,HiddenBlockFixture.HASH);
        assertEquals(NovelCatalogServiceDescribe.describe(plain,head),NovelCatalogServiceDescribe.describe(full,head));
        assertEquals(4,NovelCatalogServiceDescribe.describe(full,head).blockCount());
        var stored = new StoredRevision(full,0,HiddenBlockFixture.HASH);
        assertEquals(4,StyleAnalysisServiceSelectBlocks.selectBlocks(stored,null,null).size());
        var note = full.liveBlocks().getFirst().id(); var last = NarrativeView.visibleBlocks(full).getLast().id();
        assertEquals(HiddenBlockException.Code.HIDDEN_BLOCK_NOT_RENDERABLE,
                assertThrows(HiddenBlockException.class,
                        () -> StyleAnalysisServiceSelectBlocks.selectBlocks(stored,note,last)).code());
        assertEquals(HiddenBlockException.Code.HIDDEN_BLOCK_NOT_RENDERABLE,
                assertThrows(HiddenBlockException.class,
                        () -> StyleAnalysisServiceSelectBlocks.selectBlocks(stored,last,note)).code());
    }

    @Test void rewriteRangesCannotEraseInterveningNotes() {
        var full = HiddenBlockFixture.revision();
        var visible = NarrativeView.visibleBlocks(full);
        assertThrows(HiddenBlockException.class,
                () -> RewriteVisibleRange.require(full,java.util.List.of(visible.get(0).id(),visible.get(1).id())));
        var guard = RewriteVisibleRange.require(full,java.util.List.of(visible.getFirst().id()));
        assertEquals(full.liveBlocks().getFirst().id(),guard.expectedPreviousBlockId());
        assertEquals(1,guard.expectedBlocks().size());
    }
}
