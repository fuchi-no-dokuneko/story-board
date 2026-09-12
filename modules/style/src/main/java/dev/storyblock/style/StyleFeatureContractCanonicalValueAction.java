package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.Map;

final class StyleFeatureContractCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleFeatureContract self)  {
        return CanonicalValues.freezeMap(Map.of(
                "additive_smoothing_alpha", self.additiveSmoothingAlpha(),
                "analyzer_version", self.analyzerVersion(),
                "feature_schema_version", self.featureSchemaVersion(),
                "normalizer_version", self.normalizerVersion(),
                "tokenizer_id", self.tokenizerId(),
                "top_k", self.topK(),
                "vocabulary_hash", self.vocabularyHash()
        ), "style_feature_contract");
    }
}
