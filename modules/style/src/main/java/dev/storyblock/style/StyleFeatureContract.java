package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleFeatureContract(
        String analyzerVersion,
        String featureSchemaVersion,
        String tokenizerId,
        String vocabularyHash,
        String normalizerVersion,
        BigDecimal additiveSmoothingAlpha,
        int topK
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "analyzer_version", "feature_schema_version", "tokenizer_id",
            "vocabulary_hash", "normalizer_version", "additive_smoothing_alpha", "top_k"
    );

    public StyleFeatureContract {
        analyzerVersion = nonBlank(analyzerVersion, "analyzerVersion");
        featureSchemaVersion = nonBlank(featureSchemaVersion, "featureSchemaVersion");
        tokenizerId = nonBlank(tokenizerId, "tokenizerId");
        if (vocabularyHash == null || !HASH.matcher(vocabularyHash).matches()) {
            throw new IllegalArgumentException("Style vocabulary hash must be lowercase SHA-256");
        }
        normalizerVersion = nonBlank(normalizerVersion, "normalizerVersion");
        if (additiveSmoothingAlpha == null
                || additiveSmoothingAlpha.signum() <= 0
                || additiveSmoothingAlpha.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Style smoothing alpha must be in (0, 1]");
        }
        additiveSmoothingAlpha = additiveSmoothingAlpha.stripTrailingZeros();
        if (topK < 10 || topK > 10_000) {
            throw new IllegalArgumentException("Style Top-K must be between 10 and 10000");
        }
    }

    public static StyleFeatureContract defaults(String vocabularyHash) {
        return new StyleFeatureContract(
                StyleModule.VERSION,
                StyleModule.FEATURE_SCHEMA_VERSION,
                StyleModule.TOKENIZER_ID,
                vocabularyHash,
                StyleModule.NORMALIZER_VERSION,
                new BigDecimal("0.000001"),
                500
        );
    }

    public static StyleFeatureContract fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_feature_contract");
        return new StyleFeatureContract(
                StyleCanonical.string(value, "analyzer_version", "style_feature_contract"),
                StyleCanonical.string(
                        value, "feature_schema_version", "style_feature_contract"
                ),
                StyleCanonical.string(value, "tokenizer_id", "style_feature_contract"),
                StyleCanonical.string(value, "vocabulary_hash", "style_feature_contract"),
                StyleCanonical.string(
                        value, "normalizer_version", "style_feature_contract"
                ),
                StyleCanonical.decimal(
                        value, "additive_smoothing_alpha", "style_feature_contract"
                ),
                StyleCanonical.integer(value, "top_k", "style_feature_contract")
        );
    }

    public String contractHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "additive_smoothing_alpha", additiveSmoothingAlpha,
                "analyzer_version", analyzerVersion,
                "feature_schema_version", featureSchemaVersion,
                "normalizer_version", normalizerVersion,
                "tokenizer_id", tokenizerId,
                "top_k", topK,
                "vocabulary_hash", vocabularyHash
        ), "style_feature_contract");
    }

    private static String nonBlank(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Style " + field + " is invalid");
        }
        return value;
    }
}
