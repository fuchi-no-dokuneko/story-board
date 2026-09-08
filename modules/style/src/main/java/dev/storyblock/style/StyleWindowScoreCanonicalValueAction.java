package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleWindowScoreCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleWindowScore self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channels", self.channels().stream()
                .map(StyleCalibratedChannelScore::canonicalValue).toList());
        value.put("distance_report", self.distanceReport().canonicalValue());
        value.put("profile_selection", self.profileSelection().canonicalValue());
        value.put("window", self.window().canonicalValue());
        return CanonicalValues.freezeMap(value, "style_window_score");
    }
}
