package dev.storyblock.rewrite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.*;
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
  static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9._:/-]{1,128}");
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  static final Set<String> FIELDS = Set.of(
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
    return RewriteTextProposalFromCanonicalFactory.fromCanonical(value);
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

  Map<String, Object> valueWithoutProposalHash() {
    return RewriteTextProposalValueWithoutProposalHashAction.valueWithoutProposalHash(this);
  }
}
