package dev.storyblock.worker.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisTrace;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleWorkerClientResultBody {
    static byte[] resultBody(StyleAnalysisCompletionCommand completion) {
        StyleAnalysisTrace trace = completion.trace();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analyzer_contract_hash", completion.analyzerContractHash());
        result.put("attempt", completion.attempt());
        result.put("completed_at", completion.completedAt().toString());
        result.put("lease_owner", completion.leaseOwner());
        result.put("profile_version_hash", completion.profileVersionHash());
        result.put("snapshot_hash", completion.snapshotHash());
        result.put("summary", completion.summary().canonicalValue());
        result.put("trace", Map.of(
                "codec", StyleAnalysisTrace.CODEC,
                "content_base64", Base64.getEncoder().encodeToString(
                        trace.compressedContent()
                ),
                "content_hash", trace.contentHash(),
                "uncompressed_bytes", trace.uncompressedBytes()
        ));
        result.put("window_configuration_hash", completion.windowConfigurationHash());
        result.put("windows", completion.windows().stream()
                .map(value -> value.canonicalValue()).toList());
        return CanonicalJson.bytes(result);
    }
}
