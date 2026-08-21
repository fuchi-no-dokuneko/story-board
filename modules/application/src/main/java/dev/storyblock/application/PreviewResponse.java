package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.validator.ValidationIssue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PreviewResponse(
        Ids.RevisionId baseRevisionId,
        String baseHash,
        Map<String, Object> normalizedOperation,
        String candidateHash,
        RevisionDiff diff,
        RenderPacket renderPacket,
        List<ValidationIssue> violations,
        List<ValidationIssue> warnings,
        boolean committable
) {
    public PreviewResponse {
        Objects.requireNonNull(baseRevisionId, "baseRevisionId");
        Objects.requireNonNull(baseHash, "baseHash");
        normalizedOperation = Map.copyOf(normalizedOperation);
        Objects.requireNonNull(candidateHash, "candidateHash");
        Objects.requireNonNull(diff, "diff");
        violations = List.copyOf(violations);
        warnings = List.copyOf(warnings);
        if (committable != violations.isEmpty()) {
            throw new IllegalArgumentException("Committability must reflect validation violations");
        }
    }

    public Map<String, Object> contractFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("base_revision_id", baseRevisionId.value());
        fields.put("base_hash", baseHash);
        fields.put("normalized_operation", normalizedOperation);
        fields.put("candidate_hash", candidateHash);
        fields.put("diff", diff);
        fields.put("render_packet", renderPacket);
        fields.put("violations", violations);
        fields.put("warnings", warnings);
        fields.put("committable", committable);
        return java.util.Collections.unmodifiableMap(fields);
    }
}
