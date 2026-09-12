package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleStratumCalibrationCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleStratumCalibration self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channels", self.channels().stream()
                .map(StyleChannelCalibration::canonicalValue).toList());
        value.put("confidence", self.confidence().canonicalName());
        value.put("stratum", self.stratum().canonicalValue());
        value.put("window_count", self.windowCount());
        return CanonicalValues.freezeMap(value, "style_stratum_calibration");
    }
}
