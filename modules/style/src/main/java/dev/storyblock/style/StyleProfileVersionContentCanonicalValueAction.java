package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleProfileVersionContentCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleProfileVersionContent self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("calibration_statistics", self.calibrationStatistics());
        value.put("corpus_sources", self.corpusSources().stream()
                .map(StyleCorpusSource::canonicalValue).toList());
        value.put("feature_set", self.featureSet().canonicalValue());
        value.put("scope", self.scope().canonicalValue());
        value.put("window_configuration", self.windowConfiguration().canonicalValue());
        return CanonicalValues.freezeMap(value, "style_profile_version_content");
    }
}
