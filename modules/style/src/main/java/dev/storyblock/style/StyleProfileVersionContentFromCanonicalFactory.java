package dev.storyblock.style;

import java.util.Map;

final class StyleProfileVersionContentFromCanonicalFactory {
    static StyleProfileVersionContent fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleProfileVersionContent.FIELDS, "style_profile_version_content");
        return new StyleProfileVersionContent(
                StyleProfileScope.fromCanonical(StyleCanonical.object(
                        value.get("scope"), "style_profile_version_content.scope"
                )),
                StyleCanonical.objects(
                        value.get("corpus_sources"),
                        "style_profile_version_content.corpus_sources"
                ).stream().map(StyleCorpusSource::fromCanonical).toList(),
                StyleFeatureSet.fromCanonical(StyleCanonical.object(
                        value.get("feature_set"),
                        "style_profile_version_content.feature_set"
                )),
                StyleWindowConfiguration.fromCanonical(StyleCanonical.object(
                        value.get("window_configuration"),
                        "style_profile_version_content.window_configuration"
                )),
                StyleCanonical.object(
                        value.get("calibration_statistics"),
                        "style_profile_version_content.calibration_statistics"
                )
        );
    }
}
