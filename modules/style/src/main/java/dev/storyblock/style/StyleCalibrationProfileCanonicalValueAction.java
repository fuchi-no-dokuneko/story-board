package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleCalibrationProfileCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleCalibrationProfile self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("calibration_schema_version", self.calibrationSchemaVersion());
        value.put("contract_hash", self.contractHash());
        value.put("strata", self.strata().stream()
                .map(StyleStratumCalibration::canonicalValue).toList());
        value.put("target_corpus_hash", self.targetCorpusHash());
        value.put("window_configuration_hash", self.windowConfigurationHash());
        return CanonicalValues.freezeMap(value, "style_calibration_profile");
    }
}
