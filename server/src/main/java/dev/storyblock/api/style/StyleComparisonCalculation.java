package dev.storyblock.api.style;

import dev.storyblock.application.NovelReadCache;
import dev.storyblock.style.*;
import java.util.*;

final class StyleComparisonCalculation {
    static Map<String,Object> calculate(String novelId, NovelReadCache.Read read, List<StyleBenchmark> baselines) {
        var revision = read.revision();
        var analyzer = new StyleFeatureAnalyzer();
        var lexicon = StyleMaskingLexicon.empty();
        var features = analyzer.extract(revision.manifest().liveBlocks(), lexicon,
                StyleFeatureContract.defaults(lexicon.vocabularyHash()));
        var results = new ArrayList<Map<String, Object>>();
        for (var baseline : baselines) {
            var value = new LinkedHashMap<>(baseline.publicValue(null));
            value.put("scores", analyzer.compare(baseline.features(), features).channels().stream()
                    .map(StyleChannelDistance::canonicalValue).toList());
            value.put("reference_metadata", "text_only");
            results.add(value);
        }
        return Map.of("novel_id", novelId, "revision_id", revision.manifest().id().value(),
                "revision_hash", revision.contentHash(), "comparisons", results);
    }
}
