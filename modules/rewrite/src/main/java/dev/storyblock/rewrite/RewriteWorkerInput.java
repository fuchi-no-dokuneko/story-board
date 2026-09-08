package dev.storyblock.rewrite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.UnicodeText;
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
    if (blocks.isEmpty() || blocks.size() > RewriteModule.MAX_SOURCE_BLOCKS
        || new HashSet<>(blocks.stream().map(
            RewriteSourceBlock::blockId
        ).toList()).size() != blocks.size()
        || new HashSet<>(blocks.stream().map(
            RewriteSourceBlock::blockVersionId
        ).toList()).size() != blocks.size()) {
      throw new IllegalArgumentException("Rewrite source block range is invalid");
    }
    int firstEditable = -1;
    int lastEditable = -1;
    int editableCount = 0;
    int totalGraphemes = 0;
    for (int index = 0; index < blocks.size(); index++) {
      RewriteSourceBlock block = blocks.get(index);
      totalGraphemes += UnicodeText.graphemeCount(block.text());
      if (block.editable()) {
        if (firstEditable < 0) {
          firstEditable = index;
        }
        lastEditable = index;
        editableCount++;
      }
    }
    if (editableCount < 1 || editableCount > RewriteModule.MAX_EDITABLE_BLOCKS
        || firstEditable > RewriteModule.MAX_CONTEXT_BLOCKS_PER_SIDE
        || blocks.size() - lastEditable - 1
        > RewriteModule.MAX_CONTEXT_BLOCKS_PER_SIDE
        || totalGraphemes > RewriteModule.MAX_SOURCE_BLOCKS
        * UnicodeText.MAX_BLOCK_GRAPHEMES) {
      throw new IllegalArgumentException("Rewrite editable range is not minimal");
    }
    for (int index = firstEditable; index <= lastEditable; index++) {
      if (!blocks.get(index).editable()) {
        throw new IllegalArgumentException(
            "Rewrite editable blocks must form one contiguous range"
        );
      }
    }
    Objects.requireNonNull(constraints, "constraints");
    if (constraints.maxChangedBlocks() > editableCount
        || constraints.maxOutputGraphemes()
        > editableCount * UnicodeText.MAX_BLOCK_GRAPHEMES) {
      throw new IllegalArgumentException(
          "Rewrite constraints exceed the editable source range"
      );
    }
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
