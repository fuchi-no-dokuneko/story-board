package dev.storyblock.style;

import dev.storyblock.domain.RevisionManifest;
import java.util.List;

public final class StyleWindowPlanner {
    public List<StyleWindow> plan(
            RevisionManifest revision,
            StyleWindowConfiguration configuration
    ) {
        return StyleWindowPlannerPlanAction.plan(this, revision, configuration);
    }

    public List<StyleWindow> plan(
            List<StyleAnalysisBlock> analysisBlocks,
            StyleWindowConfiguration configuration
    ) {
        return StyleWindowPlannerPlanAction.plan(this, analysisBlocks, configuration);
    }

    record Segment(SegmentKey key, List<StyleAnalysisBlock> blocks) {
    }

    record SegmentKey(
            StyleStratumKind stratumKind,
            String pov,
            String narrativeMode,
            String shiftMarker,
            String shiftReason
    ) {
        static SegmentKey from(StyleAnalysisBlock block) {
            String marker = block.intentionalStyleShiftReason() == null
                    ? null : block.sceneId().value();
            return new SegmentKey(
                    block.stratumKind(),
                    block.pov(),
                    block.narrativeMode(),
                    marker,
                    block.intentionalStyleShiftReason()
            );
        }
    }
}
