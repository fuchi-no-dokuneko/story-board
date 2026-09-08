package dev.storyblock.style;

import java.util.Map;

final class StyleFeatureContractFromCanonicalFactory {
    static StyleFeatureContract fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleFeatureContract.FIELDS, "style_feature_contract");
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
}
