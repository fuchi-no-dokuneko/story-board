package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.style.StyleCorpusSource;
import dev.storyblock.style.StyleFeatureAnalyzer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record RewriteReferenceCorpus(
        StyleCorpusSource source,
        List<NarrativeBlock> blocks
) {
    public RewriteReferenceCorpus {
        Objects.requireNonNull(source, "source");
        blocks = List.copyOf(blocks);
        if (blocks.isEmpty() || blocks.size() > 1_000
                || new HashSet<>(blocks.stream().map(NarrativeBlock::id).toList())
                .size() != blocks.size()
                || !StyleFeatureAnalyzer.sourceHash(blocks).equals(
                        source.contentHash()
                )) {
            throw new RewriteRiskPolicyException(
                    "Rewrite reference corpus does not match its approved content hash"
            );
        }
    }
}
