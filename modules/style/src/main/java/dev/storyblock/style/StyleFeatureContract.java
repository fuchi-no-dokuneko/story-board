package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
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
    static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    static final Set<String> FIELDS = Set.of(
            "analyzer_version", "feature_schema_version", "tokenizer_id",
            "vocabulary_hash", "normalizer_version", "additive_smoothing_alpha", "top_k"
    );

    public StyleFeatureContract {
        analyzerVersion = StyleFeatureContractNonBlank.nonBlank(analyzerVersion, "analyzerVersion");
        featureSchemaVersion = StyleFeatureContractNonBlank.nonBlank(featureSchemaVersion, "featureSchemaVersion");
        tokenizerId = StyleFeatureContractNonBlank.nonBlank(tokenizerId, "tokenizerId");
        if (vocabularyHash == null || !HASH.matcher(vocabularyHash).matches()) {
            throw new IllegalArgumentException("Style vocabulary hash must be lowercase SHA-256");
        }
        normalizerVersion = StyleFeatureContractNonBlank.nonBlank(normalizerVersion, "normalizerVersion");
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
        return StyleFeatureContractFromCanonicalFactory.fromCanonical(value);
    }

    public String contractHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        return StyleFeatureContractCanonicalValueAction.canonicalValue(this);
    }

}
