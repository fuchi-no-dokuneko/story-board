package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.util.Map;

final class StyleAnalysisSnapshotFromCanonicalFactory {
    static StyleAnalysisSnapshot fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleAnalysisSnapshot.FIELDS, "style_analysis_snapshot");
        return new StyleAnalysisSnapshot(
                new Ids.NovelId(StyleCanonical.string(
                        value, "novel_id", "style_analysis_snapshot"
                )),
                new Ids.RevisionId(StyleCanonical.string(
                        value, "revision_id", "style_analysis_snapshot"
                )),
                StyleCanonical.string(value, "revision_hash", "style_analysis_snapshot"),
                StyleProfileVersion.fromCanonical(StyleCanonical.object(
                        value.get("profile_version"),
                        "style_analysis_snapshot.profile_version"
                )),
                StyleMaskingLexicon.fromCanonical(StyleCanonical.object(
                        value.get("masking_lexicon"),
                        "style_analysis_snapshot.masking_lexicon"
                )),
                StyleCanonical.objects(
                        value.get("blocks"), "style_analysis_snapshot.blocks"
                ).stream().map(StyleAnalysisBlock::fromCanonical).toList()
        );
    }
}
