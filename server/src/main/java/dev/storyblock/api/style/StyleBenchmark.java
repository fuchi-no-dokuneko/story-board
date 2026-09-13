package dev.storyblock.api.style;

import dev.storyblock.domain.UnicodeText;
import dev.storyblock.style.*;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

record StyleBenchmark(StyleDefinition definition, List<StyleSourceFiles.Stamp> sources,
        StyleFeatureSet features, long graphemes, Instant calculatedAt) {
    static StyleBenchmark calculate(StyleDefinition definition, List<StyleSourceFiles.Stamp> sources) throws IOException {
        var blocks = new ArrayList<ReferenceText>();
        for (var source : sources) {
            String text = Files.readString(source.path());
            if (!text.isBlank()) blocks.addAll(ReferenceSegments.split(text));
        }
        if (blocks.isEmpty()) throw new IOException("Reference corpus is empty");
        var lexicon = StyleMaskingLexicon.empty();
        var features = new StyleFeatureAnalyzer().extract(blocks, lexicon,
                StyleFeatureContract.defaults(lexicon.vocabularyHash()));
        if (!sources.equals(StyleSourceFiles.inspect(definition)))
            throw new IOException("Reference files changed during calculation; retry on next scan");
        return new StyleBenchmark(definition, sources, features,
                blocks.stream().mapToLong(b -> UnicodeText.graphemeCount(b.text())).sum(), Instant.now());
    }

    Map<String, Object> publicValue(String error) {
        var value = new LinkedHashMap<>(definition.publicValue());
        value.put("status", error == null ? "ready" : "stale");
        value.put("reference_file_count", sources.size());
        value.put("reference_graphemes", graphemes);
        value.put("benchmark_updated_at", calculatedAt.toString());
        if (error != null) value.put("error", error);
        return value;
    }
}
