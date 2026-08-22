package dev.storyblock.style;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StyleAnalysisCompletionCommandTest {
    @Test
    void validatesDecisionCountsByNumericValueBeyondIntegerCache() {
        Instant completedAt = Instant.parse("2026-08-21T16:30:00Z");
        Ids.StyleAnalysisId analysisId = Ids.StyleAnalysisId.create();
        List<StyleAnalysisWindowFinding> windows = new ArrayList<>();
        for (int ordinal = 0; ordinal < 128; ordinal++) {
            windows.add(new StyleAnalysisWindowFinding(
                    ordinal,
                    CanonicalJson.hash(Map.of("window", ordinal)),
                    List.of(Ids.BlockId.create()),
                    StyleDecisionState.NORMAL,
                    StyleCalibrationConfidence.CALIBRATED,
                    false,
                    Map.of("decision", "normal")
            ));
        }
        StyleAnalysisTrace trace = StyleAnalysisTrace.create(
                analysisId,
                Map.of("window_count", windows.size()),
                completedAt,
                completedAt.plus(Duration.ofDays(1))
        );

        StyleAnalysisCompletionCommand command = assertDoesNotThrow(() ->
                new StyleAnalysisCompletionCommand(
                        Ids.JobId.create(),
                        "style-worker-test",
                        1,
                        CanonicalJson.hash("running-status"),
                        CanonicalJson.hash("snapshot"),
                        CanonicalJson.hash("profile"),
                        CanonicalJson.hash("analyzer"),
                        CanonicalJson.hash("windows"),
                        new StyleAnalysisSummary(
                                128,
                                128,
                                128,
                                Map.of(StyleDecisionState.NORMAL, 128)
                        ),
                        windows,
                        trace,
                        "completion-over-integer-cache",
                        completedAt
                )
        );
        assertTrue(command.resultHash().startsWith("sha256:"));
    }
}
