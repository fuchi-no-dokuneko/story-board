package dev.storyblock.rewrite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.UnicodeText;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
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
        requireHash(revisionHash, "revision");
        Objects.requireNonNull(profileVersionId, "profileVersionId");
        requireHash(profileVersionHash, "profile version");
        requireHash(analyzerContractHash, "analyzer contract");
        requireHash(windowConfigurationHash, "window configuration");
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
        RewriteCanonical.requireKeys(value, FIELDS, "rewrite_worker_input");
        if (!RewriteModule.INPUT_SCHEMA_VERSION.equals(RewriteCanonical.string(
                value, "schema_version", "rewrite_worker_input"
        ))) {
            throw new IllegalArgumentException("Rewrite input schema version is unsupported");
        }
        return new RewriteWorkerInput(
                new Ids.ProposalId(RewriteCanonical.string(
                        value, "proposal_id", "rewrite_worker_input"
                )),
                new Ids.StyleAnalysisId(RewriteCanonical.string(
                        value, "analysis_id", "rewrite_worker_input"
                )),
                new Ids.NovelId(RewriteCanonical.string(
                        value, "novel_id", "rewrite_worker_input"
                )),
                new Ids.RevisionId(RewriteCanonical.string(
                        value, "revision_id", "rewrite_worker_input"
                )),
                RewriteCanonical.string(value, "revision_hash", "rewrite_worker_input"),
                new Ids.StyleProfileVersionId(RewriteCanonical.string(
                        value, "profile_version_id", "rewrite_worker_input"
                )),
                RewriteCanonical.string(
                        value, "profile_version_hash", "rewrite_worker_input"
                ),
                RewriteCanonical.string(
                        value, "analyzer_contract_hash", "rewrite_worker_input"
                ),
                RewriteCanonical.string(
                        value, "window_configuration_hash", "rewrite_worker_input"
                ),
                RewriteCanonical.strings(
                        value.get("finding_ids"), "rewrite_worker_input.finding_ids"
                ),
                RewriteCanonical.objects(
                        value.get("blocks"), "rewrite_worker_input.blocks"
                ).stream().map(RewriteSourceBlock::fromCanonical).toList(),
                RewriteConstraints.fromCanonical(RewriteCanonical.object(
                        value.get("constraints"), "rewrite_worker_input.constraints"
                ))
        );
    }

    public String inputHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> modelValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("blocks", blocks.stream()
                .map(RewriteSourceBlock::canonicalValue).toList());
        value.put("constraints", constraints.canonicalValue());
        value.put("input_hash", inputHash());
        value.put("schema_version", RewriteModule.INPUT_SCHEMA_VERSION);
        return CanonicalValues.freezeMap(value, "rewrite_model_input");
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", analysisId.value());
        value.put("analyzer_contract_hash", analyzerContractHash);
        value.put("blocks", blocks.stream()
                .map(RewriteSourceBlock::canonicalValue).toList());
        value.put("constraints", constraints.canonicalValue());
        value.put("finding_ids", findingIds);
        value.put("novel_id", novelId.value());
        value.put("profile_version_hash", profileVersionHash);
        value.put("profile_version_id", profileVersionId.value());
        value.put("proposal_id", proposalId.value());
        value.put("revision_hash", revisionHash);
        value.put("revision_id", revisionId.value());
        value.put("schema_version", RewriteModule.INPUT_SCHEMA_VERSION);
        value.put("window_configuration_hash", windowConfigurationHash);
        return CanonicalValues.freezeMap(value, "rewrite_worker_input");
    }

    private static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("Rewrite " + field + " hash is invalid");
        }
    }
}
