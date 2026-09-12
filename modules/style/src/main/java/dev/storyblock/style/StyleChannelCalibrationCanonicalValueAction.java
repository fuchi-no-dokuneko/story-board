package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleChannelCalibrationCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleChannelCalibration self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channel", self.channel().canonicalName());
        value.put("channel_version", self.channelVersion());
        value.put("mad", self.mad());
        value.put("median", self.median());
        value.put("primary_metric", self.primaryMetric().canonicalName());
        value.put("q95", self.q95());
        value.put("q99", self.q99());
        value.put("reference_distances", self.referenceDistances());
        return CanonicalValues.freezeMap(value, "style_channel_calibration");
    }
}
