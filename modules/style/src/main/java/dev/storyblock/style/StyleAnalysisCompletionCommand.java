package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record StyleAnalysisCompletionCommand(
        Ids.JobId jobId,
        String leaseOwner,
        int attempt,
        String expectedStatusHash,
        String snapshotHash,
        String profileVersionHash,
        String analyzerContractHash,
        String windowConfigurationHash,
        StyleAnalysisSummary summary,
        List<StyleAnalysisWindowFinding> windows,
        StyleAnalysisTrace trace,
        String idempotencyKey,
        Instant completedAt
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public StyleAnalysisCompletionCommand {
        Objects.requireNonNull(jobId, "jobId");
        if (leaseOwner == null || leaseOwner.isBlank() || leaseOwner.length() > 128
                || attempt < 1) {
            throw new IllegalArgumentException("Style completion lease identity is invalid");
        }
        requireHash(expectedStatusHash, "status");
        requireHash(snapshotHash, "snapshot");
        requireHash(profileVersionHash, "profile version");
        requireHash(analyzerContractHash, "analyzer contract");
        requireHash(windowConfigurationHash, "window configuration");
        Objects.requireNonNull(summary, "summary");
        windows = List.copyOf(windows);
        if (windows.size() != summary.operationalWindowCount()
                || windows.size() > StyleAnalysisSnapshot.MAX_BLOCKS
                || new HashSet<>(windows.stream().map(
                        StyleAnalysisWindowFinding::windowId
                ).toList()).size() != windows.size()) {
            throw new IllegalArgumentException(
                    "Style completion windows do not match the summary"
            );
        }
        EnumMap<StyleDecisionState, Integer> decisions = new EnumMap<>(
                StyleDecisionState.class
        );
        for (int index = 0; index < windows.size(); index++) {
            StyleAnalysisWindowFinding window = windows.get(index);
            if (window.ordinal() != index) {
                throw new IllegalArgumentException(
                        "Style completion window ordinals must be contiguous"
                );
            }
            decisions.merge(window.decisionState(), 1, Integer::sum);
        }
        for (StyleDecisionState state : StyleDecisionState.values()) {
            if (summary.decisionCounts().get(state).intValue()
                    != decisions.getOrDefault(state, 0).intValue()) {
                throw new IllegalArgumentException(
                        "Style completion decision totals do not match windows"
                );
            }
        }
        Objects.requireNonNull(trace, "trace");
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException(
                    "Style completion idempotency key is invalid"
            );
        }
        Objects.requireNonNull(completedAt, "completedAt");
        if (!trace.createdAt().equals(completedAt)) {
            throw new IllegalArgumentException(
                    "Style trace and completion timestamps must match"
            );
        }
    }

    public String resultHash() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", trace.analysisId().value());
        value.put("analyzer_contract_hash", analyzerContractHash);
        value.put("profile_version_hash", profileVersionHash);
        value.put("snapshot_hash", snapshotHash);
        value.put("summary", summary.canonicalValue());
        value.put("trace", trace.metadataValue());
        value.put("window_configuration_hash", windowConfigurationHash);
        value.put("windows", windows.stream()
                .map(StyleAnalysisWindowFinding::canonicalValue).toList());
        return CanonicalJson.hash(value);
    }

    public String requestHash() {
        return CanonicalJson.hash(Map.of(
                "attempt", attempt,
                "job_id", jobId.value(),
                "lease_owner", leaseOwner,
                "result_hash", resultHash()
        ));
    }

    private static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Style completion " + field + " hash is invalid"
            );
        }
    }
}
