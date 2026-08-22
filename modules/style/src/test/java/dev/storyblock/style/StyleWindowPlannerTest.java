package dev.storyblock.style;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StyleWindowPlannerTest {
    @Test
    void povResetsSegmentsAndWindowKindsHaveFixedCapabilities() {
        List<StyleAnalysisBlock> blocks = new ArrayList<>();
        Ids.SceneId firstScene = Ids.SceneId.create();
        Ids.SceneId shiftedScene = Ids.SceneId.create();
        for (int index = 0; index < 70; index++) {
            boolean shifted = index >= 35;
            blocks.add(new StyleAnalysisBlock(
                    shifted ? shiftedScene : firstScene,
                    block(index, "字".repeat(90) + "。"),
                    StyleStratumKind.NARRATION,
                    null,
                    shifted ? "pov_b" : "pov_a",
                    "narration",
                    shifted ? "Intentional dream sequence" : null
            ));
        }

        List<StyleWindow> windows = new StyleWindowPlanner().plan(
                blocks, StyleWindowConfiguration.defaults()
        );

        assertTrue(windows.stream().anyMatch(window ->
                window.kind() == StyleWindowKind.OPERATIONAL && window.fullSized()
        ));
        assertTrue(windows.stream().anyMatch(window ->
                window.kind() == StyleWindowKind.NON_OVERLAP
                        && window.sustainmentEligible()
        ));
        assertTrue(windows.stream().anyMatch(StyleWindow::localizationOnly));
        assertTrue(windows.stream()
                .filter(StyleWindow::localizationOnly)
                .noneMatch(StyleWindow::primaryDecisionEligible));
        assertTrue(windows.stream().allMatch(window ->
                window.blockIds().stream().allMatch(id ->
                        blocks.stream()
                                .filter(sample -> sample.block().id().equals(id))
                                .allMatch(sample -> sample.pov().equals(window.pov()))
                )
        ));
        assertTrue(windows.stream()
                .filter(window -> window.segment() == 1)
                .allMatch(window -> "Intentional dream sequence".equals(
                        window.intentionalStyleShiftReason()
                )));
    }

    @Test
    void oneSpeakerSelectsSpecificStratumButConversationUsesGlobalDialogue() {
        Ids.SceneId scene = Ids.SceneId.create();
        List<StyleAnalysisBlock> oneSpeaker = samples(
                scene, List.of("speaker_a", "speaker_a", "speaker_a")
        );
        List<StyleAnalysisBlock> conversation = samples(
                scene, List.of("speaker_a", "speaker_b", "speaker_a")
        );
        StyleWindowConfiguration configuration = StyleWindowConfiguration.defaults();

        StyleWindow specific = new StyleWindowPlanner().plan(
                oneSpeaker, configuration
        ).getFirst();
        StyleWindow global = new StyleWindowPlanner().plan(
                conversation, configuration
        ).getFirst();

        assertTrue(specific.requestedStratum().speakerSpecific());
        assertTrue("speaker_a".equals(specific.requestedStratum().speakerId()));
        assertFalse(global.requestedStratum().speakerSpecific());
        assertTrue(global.requestedStratum().equals(StyleStratum.dialogue()));
    }

    private static List<StyleAnalysisBlock> samples(
            Ids.SceneId scene,
            List<String> speakers
    ) {
        List<StyleAnalysisBlock> result = new ArrayList<>();
        for (int index = 0; index < speakers.size(); index++) {
            result.add(new StyleAnalysisBlock(
                    scene,
                    block(index, "他說了一句話。"),
                    StyleStratumKind.DIALOGUE,
                    speakers.get(index),
                    "pov_a",
                    "dialogue",
                    null
            ));
        }
        return result;
    }

    private static NarrativeBlock block(int index, String text) {
        return NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(index, 100),
                text,
                BlockMetadata.empty(),
                Map.of()
        );
    }
}
