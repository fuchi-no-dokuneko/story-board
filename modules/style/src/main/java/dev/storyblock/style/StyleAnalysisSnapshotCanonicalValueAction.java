package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnalysisSnapshotCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleAnalysisSnapshot self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("blocks", self.blocks().stream()
                .map(StyleAnalysisBlock::canonicalValue).toList());
        value.put("masking_lexicon", self.maskingLexicon().canonicalValue());
        value.put("novel_id", self.novelId().value());
        value.put("profile_version", self.profileVersion().canonicalValue());
        value.put("revision_hash", self.revisionHash());
        value.put("revision_id", self.revisionId().value());
        return CanonicalValues.freezeMap(value, "style_analysis_snapshot");
    }
}
