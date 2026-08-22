package dev.storyblock.rewrite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.UnicodeText;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record RewriteCandidateBlock(
        Ids.BlockId blockId,
        Ids.BlockVersionId sourceBlockVersionId,
        String sourceTextHash,
        String proposedText,
        String proposedTextHash
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "block_id", "proposed_text", "proposed_text_hash",
            "source_block_version_id", "source_text_hash"
    );

    public RewriteCandidateBlock {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(sourceBlockVersionId, "sourceBlockVersionId");
        if (sourceTextHash == null || !HASH.matcher(sourceTextHash).matches()) {
            throw new IllegalArgumentException("Rewrite candidate source hash is invalid");
        }
        UnicodeText.validateBlock(proposedText);
        if (proposedTextHash == null || !HASH.matcher(proposedTextHash).matches()
                || !CanonicalJson.hash(proposedText).equals(proposedTextHash)
                || proposedTextHash.equals(sourceTextHash)) {
            throw new IllegalArgumentException("Rewrite candidate text hash is invalid");
        }
    }

    public static RewriteCandidateBlock create(
            RewriteSourceBlock source,
            String proposedText
    ) {
        return new RewriteCandidateBlock(
                source.blockId(),
                source.blockVersionId(),
                source.textHash(),
                proposedText,
                CanonicalJson.hash(proposedText)
        );
    }

    public static RewriteCandidateBlock fromCanonical(Map<String, Object> value) {
        RewriteCanonical.requireKeys(value, FIELDS, "rewrite_candidate_block");
        return new RewriteCandidateBlock(
                new Ids.BlockId(RewriteCanonical.string(
                        value, "block_id", "rewrite_candidate_block"
                )),
                new Ids.BlockVersionId(RewriteCanonical.string(
                        value, "source_block_version_id", "rewrite_candidate_block"
                )),
                RewriteCanonical.string(
                        value, "source_text_hash", "rewrite_candidate_block"
                ),
                RewriteCanonical.string(
                        value, "proposed_text", "rewrite_candidate_block"
                ),
                RewriteCanonical.string(
                        value, "proposed_text_hash", "rewrite_candidate_block"
                )
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block_id", blockId.value());
        value.put("proposed_text", proposedText);
        value.put("proposed_text_hash", proposedTextHash);
        value.put("source_block_version_id", sourceBlockVersionId.value());
        value.put("source_text_hash", sourceTextHash);
        return CanonicalValues.freezeMap(value, "rewrite_candidate_block");
    }
}
