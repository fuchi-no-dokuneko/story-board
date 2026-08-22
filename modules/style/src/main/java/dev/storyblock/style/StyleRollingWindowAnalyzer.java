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

public final class StyleRollingWindowAnalyzer {
    private final StyleWindowPlanner planner;
    private final StyleFeatureAnalyzer analyzer;

    public StyleRollingWindowAnalyzer() {
        this(new StyleWindowPlanner(), new StyleFeatureAnalyzer());
    }

    StyleRollingWindowAnalyzer(
            StyleWindowPlanner planner,
            StyleFeatureAnalyzer analyzer
    ) {
        this.planner = Objects.requireNonNull(planner, "planner");
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
    }

    public List<StyleWindowFeatures> analyze(
            RevisionManifest revision,
            StyleMaskingLexicon lexicon,
            StyleFeatureContract contract,
            StyleWindowConfiguration configuration
    ) {
        return analyze(revision, lexicon, contract, configuration, Map.of());
    }

    public List<StyleWindowFeatures> analyze(
            RevisionManifest revision,
            StyleMaskingLexicon lexicon,
            StyleFeatureContract contract,
            StyleWindowConfiguration configuration,
            Map<String, List<BigDecimal>> contentReducedEmbeddingsByWindow
    ) {
        Objects.requireNonNull(revision, "revision");
        Map<Ids.BlockId, NarrativeBlock> blocks = new LinkedHashMap<>();
        revision.liveBlocks().forEach(block -> blocks.put(block.id(), block));
        List<StyleWindowFeatures> result = new ArrayList<>();
        for (StyleWindow window : planner.plan(revision, configuration)) {
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
                    analyzer.extract(members, lexicon, contract, embedding)
            ));
        }
        return List.copyOf(result);
    }
}
