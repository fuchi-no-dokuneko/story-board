package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.*;
import dev.storyblock.style.StyleMaskingLexicon;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenRewritePolicyTest {
    @Test void hiddenReferenceTextNeverBecomesNearCopyOrProtectedFactEvidence() {
        String secret = "HIDDEN_SENTINEL. Alice never carried 12 keys because she concealed her motive.";
        var note = NarrativeBlock.create(Ids.BlockId.create(),OrderKey.initial(),secret,
                BlockMetadata.empty(),Map.of(BlockHidden.EXTENSION_KEY,true));
        var reference = NarrativeBlock.create(Ids.BlockId.create(),OrderKey.initial(),
                "Rain fell steadily across the narrow alley.",BlockMetadata.empty(),Map.of());
        var source = NarrativeBlock.create(Ids.BlockId.create(),OrderKey.initial(),
                "She watched the rain.",BlockMetadata.empty(),Map.of());
        var ready = RewritePolicyEvaluationTest.readyProfile(reference);
        var proposal = RewritePolicyEvaluationTest.proposal(source,secret,ready.view());
        var corpus = new RewriteReferenceCorpus(ready.source(),List.of(reference,note));
        assertEquals(List.of(reference),corpus.blocks());
        assertEquals(new RewriteNearCopyChecker().check(proposal,ready.view(),
                List.of(new RewriteReferenceCorpus(ready.source(),List.of(reference)))),
                new RewriteNearCopyChecker().check(proposal,ready.view(),List.of(corpus)));
        assertTrue(new RewriteNearCopyChecker().check(proposal,ready.view(),List.of(corpus)).isEmpty());
        var facts = new RewriteProtectedFactExtractor().snapshot(note,note.text(),
                new StyleMaskingLexicon(List.of("Alice"),List.of()));
        assertTrue(facts.facts().isEmpty()); assertTrue(facts.manualRiskReasons().isEmpty());
    }
}
