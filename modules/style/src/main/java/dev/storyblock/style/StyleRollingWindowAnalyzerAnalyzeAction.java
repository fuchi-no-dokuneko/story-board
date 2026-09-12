package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.RevisionManifest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class StyleRollingWindowAnalyzerAnalyzeAction {
    static List<StyleWindowFeatures> analyze(StyleRollingWindowAnalyzer self, RevisionManifest revision, StyleMaskingLexicon lexicon, StyleFeatureContract contract, StyleWindowConfiguration configuration, Map<String, List<BigDecimal>> contentReducedEmbeddingsByWindow)  {
        Objects.requireNonNull(revision, "revision");
        Map<Ids.BlockId, NarrativeBlock> blocks = new LinkedHashMap<>();
        revision.liveBlocks().forEach(block -> blocks.put(block.id(), block));
        return self.analyze(
                self.planner.plan(revision, configuration),
                blocks,
                lexicon,
                contract,
                contentReducedEmbeddingsByWindow
        );
    }

    static List<StyleWindowFeatures> analyze(StyleRollingWindowAnalyzer self, List<StyleWindow> windows, Map<Ids.BlockId, NarrativeBlock> blocks, StyleMaskingLexicon lexicon, StyleFeatureContract contract, Map<String, List<BigDecimal>> contentReducedEmbeddingsByWindow)  {
        List<StyleWindowFeatures> result = new ArrayList<>();
        for (StyleWindow window : windows) {
            List<NarrativeBlock> members = window.blockIds().stream()
                    .map(blocks::get)
                    .toList();
            if (members.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Style window references a block outside the revision"
                );
            }
            List<BigDecimal> embedding = contentReducedEmbeddingsByWindow.getOrDefault(
                    window.windowId(), List.of()
            );
            result.add(new StyleWindowFeatures(
                    window,
                    self.analyzer.extract(members, lexicon, contract, embedding)
            ));
        }
        return List.copyOf(result);
    }
}
