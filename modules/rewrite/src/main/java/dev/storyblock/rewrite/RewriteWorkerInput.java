package dev.storyblock.rewrite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record RewriteWorkerInput(
    Ids.ProposalId proposalId,
    Ids.StyleAnalysisId analysisId,
    Ids.NovelId novelId,
    Ids.RevisionId revisionId,
    String revisionHash,
    Ids.StyleProfileVersionId profileVersionId,
    String profileVersionHash,
    String analyzerContractHash,
    String windowConfigurationHash,
    List<String> findingIds,
    List<RewriteSourceBlock> blocks,
    RewriteConstraints constraints
) {
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  static final Set<String> FIELDS = Set.of(
      "analysis_id", "analyzer_contract_hash", "blocks", "constraints",
      "finding_ids", "novel_id", "profile_version_hash",
      "profile_version_id", "proposal_id", "revision_hash", "revision_id",
      "schema_version", "window_configuration_hash"
  );

  public RewriteWorkerInput {
    Objects.requireNonNull(proposalId, "proposalId");
    Objects.requireNonNull(analysisId, "analysisId");
    Objects.requireNonNull(novelId, "novelId");
    Objects.requireNonNull(revisionId, "revisionId");
    RewriteWorkerInputRequireHash.requireHash(revisionHash, "revision");
    Objects.requireNonNull(profileVersionId, "profileVersionId");
    RewriteWorkerInputRequireHash.requireHash(profileVersionHash, "profile version");
    RewriteWorkerInputRequireHash.requireHash(analyzerContractHash, "analyzer contract");
    RewriteWorkerInputRequireHash.requireHash(windowConfigurationHash, "window configuration");
    findingIds = List.copyOf(findingIds);
    if (findingIds.isEmpty() || findingIds.size() > RewriteModule.MAX_FINDINGS
        || new HashSet<>(findingIds).size() != findingIds.size()
        || findingIds.stream().anyMatch(id -> !HASH.matcher(id).matches())) {
      throw new IllegalArgumentException("Rewrite finding IDs are invalid");
    }
    blocks = List.copyOf(blocks);
    RewriteWorkerInputValidation.validate(blocks, constraints);
  }

  public static RewriteWorkerInput fromCanonical(Map<String, Object> value) {
    return RewriteWorkerInputFromCanonicalFactory.fromCanonical(value);
  }

  public String inputHash() {
    return CanonicalJson.hash(canonicalValue());
  }

  public Map<String, Object> modelValue() {
    return RewriteWorkerInputModelValueAction.modelValue(this);
  }

  public Map<String, Object> canonicalValue() {
    return RewriteWorkerInputCanonicalValueAction.canonicalValue(this);
  }

}
