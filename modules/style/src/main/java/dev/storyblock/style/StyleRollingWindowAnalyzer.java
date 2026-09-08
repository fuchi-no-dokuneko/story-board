package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.RevisionManifest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StyleRollingWindowAnalyzer {
    final StyleWindowPlanner planner;
    final StyleFeatureAnalyzer analyzer;

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
            List<StyleAnalysisBlock> blocks,
            StyleMaskingLexicon lexicon,
            StyleFeatureContract contract,
            StyleWindowConfiguration configuration
    ) {
        Objects.requireNonNull(blocks, "blocks");
        Map<Ids.BlockId, NarrativeBlock> source = new LinkedHashMap<>();
        blocks.forEach(block -> source.put(block.block().id(), block.block()));
        return analyze(
                planner.plan(blocks, configuration),
                source,
                lexicon,
                contract,
                Map.of()
        );
    }

    public List<StyleWindowFeatures> analyze(
            RevisionManifest revision,
            StyleMaskingLexicon lexicon,
            StyleFeatureContract contract,
            StyleWindowConfiguration configuration,
            Map<String, List<BigDecimal>> contentReducedEmbeddingsByWindow
    ) {
        return StyleRollingWindowAnalyzerAnalyzeAction.analyze(this, revision, lexicon, contract, configuration, contentReducedEmbeddingsByWindow);
    }

    List<StyleWindowFeatures> analyze(
            List<StyleWindow> windows,
            Map<Ids.BlockId, NarrativeBlock> blocks,
            StyleMaskingLexicon lexicon,
            StyleFeatureContract contract,
            Map<String, List<BigDecimal>> contentReducedEmbeddingsByWindow
    ) {
        return StyleRollingWindowAnalyzerAnalyzeAction.analyze(this, windows, blocks, lexicon, contract, contentReducedEmbeddingsByWindow);
    }
}
