package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleFeatureVectorCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleFeatureVector self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channel", self.channel().canonicalName());
        value.put("channel_version", self.channelVersion());
        value.put("contract_hash", self.contractHash());
        value.put("distribution", self.distribution());
        value.put("measurements", self.measurements());
        value.put("embedding", self.embedding());
        return CanonicalValues.freezeMap(value, "style_feature_vector");
    }
}
