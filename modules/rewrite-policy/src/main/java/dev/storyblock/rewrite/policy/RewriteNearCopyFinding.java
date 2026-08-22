package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleCorpusSourceKind;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record RewriteNearCopyFinding(
        Ids.BlockId candidateBlockId,
        String sourceId,
        StyleCorpusSourceKind sourceKind,
        String ngramHash,
        int ngramGraphemes,
        int matchedNgramCount,
        int candidateNgramCount,
        NearCopyDisposition disposition
) {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public RewriteNearCopyFinding {
        Objects.requireNonNull(candidateBlockId, "candidateBlockId");
        if (sourceId == null || !ID.matcher(sourceId).matches()) {
            throw new IllegalArgumentException("Near-copy source ID is invalid");
        }
        Objects.requireNonNull(sourceKind, "sourceKind");
        if (ngramHash == null || !HASH.matcher(ngramHash).matches()
                || ngramGraphemes < 2 || matchedNgramCount < 1
                || candidateNgramCount < matchedNgramCount) {
            throw new IllegalArgumentException("Near-copy evidence is invalid");
        }
        Objects.requireNonNull(disposition, "disposition");
        NearCopyDisposition expected = sourceKind == StyleCorpusSourceKind.OWNER
                || sourceKind == StyleCorpusSourceKind.PUBLIC_DOMAIN
                ? NearCopyDisposition.MANUAL_ONLY : NearCopyDisposition.BLOCK;
        if (disposition != expected) {
            throw new IllegalArgumentException(
                    "Near-copy disposition does not match corpus provenance"
            );
        }
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "candidate_block_id", candidateBlockId.value(),
                "candidate_ngram_count", candidateNgramCount,
                "disposition", disposition.canonicalName(),
                "matched_ngram_count", matchedNgramCount,
                "ngram_graphemes", ngramGraphemes,
                "ngram_hash", ngramHash,
                "source_id", sourceId,
                "source_kind", sourceKind.canonicalName()
        ), "rewrite_near_copy_finding");
    }
}
