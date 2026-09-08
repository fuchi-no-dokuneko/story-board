package dev.storyblock.rewrite;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class RewriteCandidateBlockCanonicalValueAction {
    static Map<String, Object> canonicalValue(RewriteCandidateBlock self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block_id", self.blockId().value());
        value.put("proposed_text", self.proposedText());
        value.put("proposed_text_hash", self.proposedTextHash());
        value.put("source_block_version_id", self.sourceBlockVersionId().value());
        value.put("source_text_hash", self.sourceTextHash());
        return CanonicalValues.freezeMap(value, "rewrite_candidate_block");
    }
}
