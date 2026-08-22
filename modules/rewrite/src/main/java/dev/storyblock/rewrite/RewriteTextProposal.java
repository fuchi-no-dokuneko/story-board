package dev.storyblock.rewrite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.UnicodeText;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record RewriteTextProposal(
        RewriteWorkerInput input,
        String modelId,
        String modelResponseHash,
        List<RewriteCandidateBlock> candidates,
        Instant createdAt
) {
    private static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9._:/-]{1,128}");
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "candidates", "created_at", "input", "input_hash", "model_id",
            "model_response_hash", "proposal_hash", "proposal_id", "schema_version"
    );

    public RewriteTextProposal {
        Objects.requireNonNull(input, "input");
        if (modelId == null || !MODEL_ID.matcher(modelId).matches()) {
            throw new IllegalArgumentException("Rewrite proposal model ID is invalid");
        }
        if (modelResponseHash == null || !HASH.matcher(modelResponseHash).matches()) {
            throw new IllegalArgumentException("Rewrite model response hash is invalid");
        }
        candidates = List.copyOf(candidates);
        if (candidates.isEmpty()
                || candidates.size() > input.constraints().maxChangedBlocks()
                || new HashSet<>(candidates.stream().map(
                        RewriteCandidateBlock::blockId
                ).toList()).size() != candidates.size()) {
            throw new IllegalArgumentException("Rewrite proposal candidates are invalid");
        }
        Map<Ids.BlockId, RewriteSourceBlock> sources = new HashMap<>();
        input.blocks().forEach(block -> sources.put(block.blockId(), block));
        Map<Ids.BlockId, Integer> sourceOrdinals = new HashMap<>();
        for (int index = 0; index < input.blocks().size(); index++) {
            sourceOrdinals.put(input.blocks().get(index).blockId(), index);
        }
        int proposedGraphemes = 0;
        int priorOrdinal = -1;
        for (RewriteCandidateBlock candidate : candidates) {
            RewriteSourceBlock source = sources.get(candidate.blockId());
            Integer sourceOrdinal = sourceOrdinals.get(candidate.blockId());
            if (source == null || !source.editable()
                    || !candidate.sourceBlockVersionId().equals(source.blockVersionId())
                    || !candidate.sourceTextHash().equals(source.textHash())
                    || sourceOrdinal == null || sourceOrdinal <= priorOrdinal) {
                throw new IllegalArgumentException(
                        "Rewrite candidate is not canonically bound to its source block"
                );
            }
            priorOrdinal = sourceOrdinal;
            proposedGraphemes += UnicodeText.graphemeCount(candidate.proposedText());
        }
        if (proposedGraphemes > input.constraints().maxOutputGraphemes()) {
            throw new IllegalArgumentException("Rewrite proposal exceeds its output limit");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static RewriteTextProposal fromCanonical(Map<String, Object> value) {
        RewriteCanonical.requireKeys(value, FIELDS, "rewrite_text_proposal");
        if (!RewriteModule.PROPOSAL_SCHEMA_VERSION.equals(RewriteCanonical.string(
                value, "schema_version", "rewrite_text_proposal"
        ))) {
            throw new IllegalArgumentException("Rewrite proposal schema is unsupported");
        }
        RewriteWorkerInput input = RewriteWorkerInput.fromCanonical(
                RewriteCanonical.object(value.get("input"), "rewrite_text_proposal.input")
        );
        if (!input.proposalId().value().equals(RewriteCanonical.string(
                value, "proposal_id", "rewrite_text_proposal"
        )) || !input.inputHash().equals(RewriteCanonical.string(
                value, "input_hash", "rewrite_text_proposal"
        ))) {
            throw new IllegalArgumentException("Rewrite proposal input binding is invalid");
        }
        RewriteTextProposal proposal = new RewriteTextProposal(
                input,
                RewriteCanonical.string(value, "model_id", "rewrite_text_proposal"),
                RewriteCanonical.string(
                        value, "model_response_hash", "rewrite_text_proposal"
                ),
                RewriteCanonical.objects(
                        value.get("candidates"), "rewrite_text_proposal.candidates"
                ).stream().map(RewriteCandidateBlock::fromCanonical).toList(),
                RewriteCanonical.instant(
                        value, "created_at", "rewrite_text_proposal"
                )
        );
        if (!proposal.proposalHash().equals(RewriteCanonical.string(
                value, "proposal_hash", "rewrite_text_proposal"
        ))) {
            throw new IllegalArgumentException("Rewrite proposal hash is invalid");
        }
        return proposal;
    }

    public Ids.ProposalId proposalId() {
        return input.proposalId();
    }

    public String proposalHash() {
        return CanonicalJson.hash(valueWithoutProposalHash());
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>(valueWithoutProposalHash());
        value.put("proposal_hash", proposalHash());
        return CanonicalValues.freezeMap(value, "rewrite_text_proposal");
    }

    private Map<String, Object> valueWithoutProposalHash() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("candidates", candidates.stream()
                .map(RewriteCandidateBlock::canonicalValue).toList());
        value.put("created_at", createdAt.toString());
        value.put("input", input.canonicalValue());
        value.put("input_hash", input.inputHash());
        value.put("model_id", modelId);
        value.put("model_response_hash", modelResponseHash);
        value.put("proposal_id", input.proposalId().value());
        value.put("schema_version", RewriteModule.PROPOSAL_SCHEMA_VERSION);
        return CanonicalValues.freezeMap(value, "rewrite_text_proposal_content");
    }
}
