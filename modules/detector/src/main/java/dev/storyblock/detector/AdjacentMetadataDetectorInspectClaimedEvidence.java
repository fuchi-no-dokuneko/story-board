package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;
import static dev.storyblock.detector.AdjacentMetadataDetector.OBSERVATION_FIELDS;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorInspectClaimedEvidence {
    static void inspectClaimedEvidence(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            List<DetectorFinding> findings
    ) {
        Map<String, Object> local = current.rendered().localMetadata().fields();
        for (String field : OBSERVATION_FIELDS) {
            Object observation = local.get(field);
            if (observation instanceof Map<?, ?> map && map.containsKey("evidence")
                    && !AdjacentMetadataDetectorMatchesEvidence.matchesEvidence(current.text(), map.get("evidence"))) {
                AdjacentMetadataDetectorAddEvidenceMismatch.addEvidenceMismatch(
                        revision,
                        packet,
                        current,
                        contextIds,
                        field + ".evidence",
                        findings
                );
            }
        }

        Object events = local.get("presence_events");
        if (events instanceof List<?> entries) {
            for (int index = 0; index < entries.size(); index++) {
                Object entry = entries.get(index);
                if (entry instanceof Map<?, ?> event && event.containsKey("evidence")
                        && !AdjacentMetadataDetectorMatchesEvidence.matchesEvidence(current.text(), event.get("evidence"))) {
                    AdjacentMetadataDetectorAddEvidenceMismatch.addEvidenceMismatch(
                            revision,
                            packet,
                            current,
                            contextIds,
                            "presence_events[" + index + "].evidence",
                            findings
                    );
                }
            }
        }

        Object provenance = local.get("provenance");
        if (provenance instanceof Map<?, ?> map && map.get("evidence") instanceof List<?> entries) {
            for (int index = 0; index < entries.size(); index++) {
                if (!AdjacentMetadataDetectorMatchesEvidence.matchesEvidence(current.text(), entries.get(index))) {
                    AdjacentMetadataDetectorAddEvidenceMismatch.addEvidenceMismatch(
                            revision,
                            packet,
                            current,
                            contextIds,
                            "provenance.evidence[" + index + "]",
                            findings
                    );
                }
            }
        }
    }
}
