package dev.storyblock.style;

import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static dev.storyblock.style.StyleWindowPlanner.Segment;

final class StyleWindowPlannerPlanAction {
    static List<StyleWindow> plan(StyleWindowPlanner self, RevisionManifest revision, StyleWindowConfiguration configuration)  {
        Objects.requireNonNull(revision, "revision");
        List<StyleAnalysisBlock> blocks = new ArrayList<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                scene.blocks().forEach(block ->
                        blocks.add(StyleAnalysisBlock.from(scene, block))
                );
            }
        }
        return self.plan(blocks, configuration);
    }

    static List<StyleWindow> plan(StyleWindowPlanner self, List<StyleAnalysisBlock> analysisBlocks, StyleWindowConfiguration configuration)  {
        analysisBlocks = List.copyOf(analysisBlocks);
        Objects.requireNonNull(configuration, "configuration");
        if (analysisBlocks.isEmpty() || analysisBlocks.size() > 1_000) {
            throw new IllegalArgumentException(
                    "Style window planning requires 1 to 1000 blocks"
            );
        }
        List<Segment> segments = StyleWindowPlannerSegments.segments(analysisBlocks);
        List<StyleWindow> result = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            Segment segment = segments.get(index);
            result.addAll(StyleWindowPlannerWindows.windows(
                    segment,
                    index,
                    StyleWindowKind.OPERATIONAL,
                    configuration.operationalGraphemes(),
                    configuration.operationalStrideGraphemes()
            ));
            result.addAll(StyleWindowPlannerWindows.windows(
                    segment,
                    index,
                    StyleWindowKind.MICRO,
                    configuration.microGraphemes(),
                    configuration.microStrideGraphemes()
            ));
            if (configuration.nonOverlapEnabled()) {
                result.addAll(StyleWindowPlannerWindows.windows(
                        segment,
                        index,
                        StyleWindowKind.NON_OVERLAP,
                        configuration.operationalGraphemes(),
                        configuration.operationalGraphemes()
                ));
            }
        }
        return List.copyOf(result);
    }
}
