package dev.storyblock.rewrite;

import dev.storyblock.domain.CanonicalValues;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record RewriteModelResponse(
        String modelId,
        String inputHash,
        List<RewriteModelReplacement> replacements
) {
    private static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9._:/-]{1,128}");
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "model", "output", "protocol_version"
    );
    private static final Set<String> OUTPUT_FIELDS = Set.of(
            "input_hash", "replacements"
    );

    public RewriteModelResponse {
        if (modelId == null || !MODEL_ID.matcher(modelId).matches()) {
            throw new IllegalArgumentException("Rewrite model ID is invalid");
        }
        if (inputHash == null || !HASH.matcher(inputHash).matches()) {
            throw new IllegalArgumentException("Rewrite model input hash is invalid");
        }
        replacements = List.copyOf(replacements);
        if (replacements.isEmpty()
                || replacements.size() > RewriteModule.MAX_EDITABLE_BLOCKS
                || new HashSet<>(replacements.stream().map(
                        RewriteModelReplacement::blockId
                ).toList()).size() != replacements.size()) {
            throw new IllegalArgumentException("Rewrite model replacements are invalid");
        }
    }

    public static RewriteModelResponse fromCanonical(Map<String, Object> value) {
        RewriteCanonical.requireKeys(value, FIELDS, "rewrite_model_response");
        if (!RewriteModule.MODEL_PROTOCOL_VERSION.equals(RewriteCanonical.string(
                value, "protocol_version", "rewrite_model_response"
        ))) {
            throw new IllegalArgumentException("Rewrite model protocol is unsupported");
        }
        Map<String, Object> output = RewriteCanonical.object(
                value.get("output"), "rewrite_model_response.output"
        );
        RewriteCanonical.requireKeys(
                output, OUTPUT_FIELDS, "rewrite_model_response.output"
        );
        return new RewriteModelResponse(
                RewriteCanonical.string(value, "model", "rewrite_model_response"),
                RewriteCanonical.string(
                        output, "input_hash", "rewrite_model_response.output"
                ),
                RewriteCanonical.objects(
                        output.get("replacements"),
                        "rewrite_model_response.output.replacements"
                ).stream().map(RewriteModelReplacement::fromCanonical).toList()
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("input_hash", inputHash);
        output.put("replacements", replacements.stream()
                .map(RewriteModelReplacement::canonicalValue).toList());
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("model", modelId);
        value.put("output", output);
        value.put("protocol_version", RewriteModule.MODEL_PROTOCOL_VERSION);
        return CanonicalValues.freezeMap(value, "rewrite_model_response");
    }
}
