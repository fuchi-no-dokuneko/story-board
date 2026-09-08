package dev.storyblock.style;

import java.util.Map;

final class StyleFeatureSetFromCanonicalFactory {
    static StyleFeatureSet fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleFeatureSet.FIELDS, "style_feature_set");
        return new StyleFeatureSet(
                StyleCanonical.string(value, "source_hash", "style_feature_set"),
                StyleFeatureContract.fromCanonical(StyleCanonical.object(
                        value.get("contract"), "style_feature_set.contract"
                )),
                StyleCanonical.objects(value.get("channels"), "style_feature_set.channels")
                        .stream().map(StyleFeatureVector::fromCanonical).toList()
        );
    }
}
