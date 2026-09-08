package dev.storyblock.style;

import java.util.Map;

final class StyleFeatureVectorFromCanonicalFactory {
    static StyleFeatureVector fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleFeatureVector.FIELDS, "style_feature_vector");
        return new StyleFeatureVector(
                StyleFeatureChannel.fromCanonicalName(StyleCanonical.string(
                        value, "channel", "style_feature_vector"
                )),
                StyleCanonical.string(value, "channel_version", "style_feature_vector"),
                StyleCanonical.string(value, "contract_hash", "style_feature_vector"),
                StyleCanonical.decimals(value.get("distribution"), "style_feature_vector.distribution"),
                StyleCanonical.decimals(value.get("measurements"), "style_feature_vector.measurements"),
                StyleCanonical.decimalList(value.get("embedding"), "style_feature_vector.embedding")
        );
    }
}
