package dev.storyblock.detector;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.renderer.RenderRange;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdjacentMetadataDetectorGoldenTest {
    private static final String HASH = "sha256:" + "b".repeat(64);

    @Test
    void everyDocumentedFindingCodeHasAGoldenCase() throws IOException {
        List<GoldenCase> cases = goldenCases();
        Set<FindingCode> covered = EnumSet.noneOf(FindingCode.class);

        for (GoldenCase golden : cases) {
            DetectorRun run = execute(golden.scenario());
            DetectorFinding finding = run.findings().stream()
                    .filter(candidate -> candidate.code() == golden.code())
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            golden.name() + " did not produce " + golden.code()
                    ));

            assertEquals(golden.severity(), finding.severity(), golden.name());
            assertEquals(HASH, finding.revisionHash(), golden.name());
            assertEquals(DetectorModule.VERSION, finding.ruleVersion(), golden.name());
            assertFalse(finding.evidence().isEmpty(), golden.name());
            assertFalse(finding.affectedSceneIds().isEmpty(), golden.name());
            covered.add(golden.code());
        }

        assertEquals(EnumSet.allOf(FindingCode.class), covered);
    }

    @Test
    void repeatedRunIsByteIdenticalAndNeverChangesCanonicalRevision() {
        RevisionManifest revision = fieldChange("location", false);
        byte[] canonicalBefore = NarrativeCanonicalMapper.toCanonical(revision).canonicalBytes();
        AdjacentMetadataDetector detector = new AdjacentMetadataDetector();

        DetectorRun first = detector.detect(revision, HASH, RenderRange.all());
        DetectorRun second = detector.detect(revision, HASH, RenderRange.all());
        byte[] canonicalAfter = NarrativeCanonicalMapper.toCanonical(revision).canonicalBytes();

        assertArrayEquals(canonicalBefore, canonicalAfter);
        assertArrayEquals(
                CanonicalJson.bytes(first.canonicalValue()),
                CanonicalJson.bytes(second.canonicalValue())
        );
        DetectorFinding location = finding(
                first, FindingCode.LOCATION_CHANGED_WITHOUT_TRANSITION
        );
        assertEquals(3, location.contextBlockIds().size());
        assertEquals(
                first.findings().stream().map(DetectorFinding::findingId).toList(),
                second.findings().stream().map(DetectorFinding::findingId).toList()
        );
    }

    @Test
    void continuousBoundaryRequiresCompatibleState() {
        DetectorRun appeared = detect(boundary(
                TransitionMode.CONTINUOUS,
                List.of("char_a"),
                List.of("char_a", "char_b")
        ));
        DetectorRun disappeared = detect(boundary(
                TransitionMode.CONTINUOUS,
                List.of("char_a"),
                List.of()
        ));
        DetectorRun compatible = detect(boundary(
                TransitionMode.CONTINUOUS,
                List.of("char_a"),
                List.of("char_a")
        ));

        assertTrue(has(appeared, FindingCode.CHARACTER_APPEARED_WITHOUT_ENTER));
        assertTrue(has(disappeared, FindingCode.CHARACTER_DISAPPEARED_WITHOUT_EXIT));
        assertEquals(List.of(), compatible.findings());
    }

    @Test
    void everyResetModeSuppressesPerCharacterTransitionErrors() {
        for (TransitionMode mode : List.of(
                TransitionMode.OPENING,
                TransitionMode.CUT,
                TransitionMode.TIME_SKIP,
                TransitionMode.FLASHBACK,
                TransitionMode.PARALLEL
        )) {
            DetectorRun run = detect(boundary(
                    mode, List.of("char_a"), List.of("char_b")
            ));

            assertTrue(has(run, FindingCode.INTENTIONAL_SCENE_RESET), mode.name());
            assertFalse(has(run, FindingCode.CHARACTER_APPEARED_WITHOUT_ENTER), mode.name());
            assertFalse(has(run, FindingCode.CHARACTER_DISAPPEARED_WITHOUT_EXIT), mode.name());
            assertEquals(
                    mode.canonicalName(),
                    finding(run, FindingCode.INTENTIONAL_SCENE_RESET)
                            .evidence().get("transition_mode")
            );
        }
    }

    @Test
    void unknownAndNotApplicableBoundaryValuesAreNotCompared() {
        RevisionManifest revision = weatherBoundary(
                Map.of("mode", "explicit", "value", "rain"),
                Map.of("mode", "not_applicable")
        );

        DetectorRun run = detect(revision);

        assertFalse(has(run, FindingCode.WEATHER_CHANGED_WITHOUT_EVIDENCE));
        assertFalse(has(run, FindingCode.INTENTIONAL_SCENE_RESET));
    }

    @Test
    void rangeUsesAdjacentContextAndOnlyItsEnteringBoundary() {
        RevisionManifest revision = boundary(
                TransitionMode.CONTINUOUS,
                List.of("char_a"),
                List.of("char_a", "char_b")
        );
        NarrativeScene first = revision.novel().chapters().getFirst().scenes().getFirst();
        NarrativeScene second = revision.novel().chapters().getFirst().scenes().getLast();

        DetectorRun firstOnly = new AdjacentMetadataDetector().detect(
                revision,
                HASH,
                RenderRange.inclusive(
                        first.blocks().getFirst().id(), first.blocks().getFirst().id()
                )
        );
        DetectorRun secondOnly = new AdjacentMetadataDetector().detect(
                revision,
                HASH,
                RenderRange.inclusive(
                        second.blocks().getFirst().id(), second.blocks().getFirst().id()
                )
        );

        assertFalse(has(firstOnly, FindingCode.CHARACTER_APPEARED_WITHOUT_ENTER));
        assertTrue(has(secondOnly, FindingCode.CHARACTER_APPEARED_WITHOUT_ENTER));
    }

    private static DetectorRun execute(String scenario) {
        return switch (scenario) {
            case "location_change" -> detect(fieldChange("location", false));
            case "character_appeared" -> detect(boundary(
                    TransitionMode.CONTINUOUS,
                    List.of("char_a"),
                    List.of("char_a", "char_b")
            ));
            case "character_disappeared" -> detect(boundary(
                    TransitionMode.CONTINUOUS,
                    List.of("char_a"),
                    List.of()
            ));
            case "weather_change" -> detect(fieldChange("weather", false));
            case "time_change" -> detect(fieldChange("time", false));
            case "pov_change" -> detect(fieldChange("pov", false));
            case "metadata_mismatch" -> detect(fieldChange("location", true));
            case "intentional_reset" -> detect(boundary(
                    TransitionMode.CUT,
                    List.of("char_a"),
                    List.of("char_b")
            ));
            default -> throw new IllegalArgumentException("Unknown golden scenario " + scenario);
        };
    }

    private static DetectorRun detect(RevisionManifest revision) {
        return new AdjacentMetadataDetector().detect(revision, HASH, RenderRange.all());
    }

    private static RevisionManifest fieldChange(String field, boolean staleEvidence) {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        Map<String, Object> seed = new java.util.LinkedHashMap<>();
        seed.put("present_character_ids", List.of());
        if (!"pov".equals(field)) {
            seed.put(field, explicit("old"));
        }

        Map<String, Object> firstMeta = "pov".equals(field)
                ? Map.of(field, explicit("old")) : Map.of();
        Map<String, Object> changed = new java.util.LinkedHashMap<>(explicit("new"));
        if (staleEvidence) {
            changed.put("evidence", Map.of(
                    "start_grapheme", 0,
                    "end_grapheme", 1,
                    "quote", "Wrong",
                    "quote_hash", "sha256:" + "0".repeat(64)
            ));
        }
        List<NarrativeBlock> blocks = List.of(
                block(0, 3, "Opening sentence.", new BlockMetadata(firstMeta)),
                block(1, 3, "Middle sentence.", new BlockMetadata(Map.of(field, changed))),
                block(2, 3, "Closing sentence.", BlockMetadata.empty())
        );
        NarrativeScene scene = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.initial(),
                "Single scene",
                TransitionMode.OPENING,
                new SceneSeed(seed),
                blocks,
                Map.of()
        );
        return revision(chapterId, List.of(scene));
    }

    private static RevisionManifest boundary(
            TransitionMode mode,
            List<String> beforeCharacters,
            List<String> afterCharacters
    ) {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        NarrativeScene first = scene(
                chapterId,
                0,
                TransitionMode.OPENING,
                beforeCharacters,
                "Before boundary."
        );
        NarrativeScene second = scene(
                chapterId,
                1,
                mode,
                afterCharacters,
                "After boundary."
        );
        return revision(chapterId, List.of(first, second));
    }

    private static RevisionManifest weatherBoundary(Object before, Object after) {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        NarrativeScene first = sceneWithSeed(
                chapterId,
                0,
                TransitionMode.OPENING,
                Map.of("weather", before, "present_character_ids", List.of()),
                "Before weather."
        );
        NarrativeScene second = sceneWithSeed(
                chapterId,
                1,
                TransitionMode.CONTINUOUS,
                Map.of("weather", after, "present_character_ids", List.of()),
                "After weather."
        );
        return revision(chapterId, List.of(first, second));
    }

    private static NarrativeScene scene(
            Ids.ChapterId chapterId,
            int index,
            TransitionMode mode,
            List<String> characters,
            String text
    ) {
        return sceneWithSeed(
                chapterId,
                index,
                mode,
                Map.of("present_character_ids", characters),
                text
        );
    }

    private static NarrativeScene sceneWithSeed(
            Ids.ChapterId chapterId,
            int index,
            TransitionMode mode,
            Map<String, Object> seed,
            String text
    ) {
        return new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.rebalanced(index, 2),
                "Scene " + index,
                mode,
                new SceneSeed(seed),
                List.of(block(0, 1, text, BlockMetadata.empty())),
                Map.of()
        );
    }

    private static NarrativeBlock block(
            int index,
            int total,
            String text,
            BlockMetadata metadata
    ) {
        return NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.rebalanced(index, total),
                text,
                metadata,
                Map.of()
        );
    }

    private static RevisionManifest revision(
            Ids.ChapterId chapterId,
            List<NarrativeScene> scenes
    ) {
        NarrativeChapter chapter = new NarrativeChapter(
                chapterId,
                OrderKey.initial(),
                "Detector chapter",
                scenes,
                Map.of()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                Instant.parse("2026-08-21T12:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(), List.of(chapter), Map.of())
        );
    }

    private static Map<String, Object> explicit(Object value) {
        return Map.of("mode", "explicit", "value", value);
    }

    private static boolean has(DetectorRun run, FindingCode code) {
        return run.findings().stream().anyMatch(finding -> finding.code() == code);
    }

    private static DetectorFinding finding(DetectorRun run, FindingCode code) {
        return run.findings().stream()
                .filter(candidate -> candidate.code() == code)
                .findFirst()
                .orElseThrow();
    }

    private static List<GoldenCase> goldenCases() throws IOException {
        try (InputStream input = AdjacentMetadataDetectorGoldenTest.class.getResourceAsStream(
                "/golden/detector-cases.json"
        )) {
            if (input == null) {
                throw new IOException("Missing detector golden cases");
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> values = CanonicalJson.parse(
                    input.readAllBytes(), List.class
            );
            List<GoldenCase> result = new ArrayList<>();
            for (Map<String, Object> value : values) {
                result.add(new GoldenCase(
                        (String) value.get("name"),
                        (String) value.get("scenario"),
                        FindingCode.valueOf((String) value.get("code")),
                        severity((String) value.get("severity"))
                ));
            }
            return List.copyOf(result);
        }
    }

    private static FindingSeverity severity(String value) {
        for (FindingSeverity severity : FindingSeverity.values()) {
            if (severity.canonicalName().equals(value)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown finding severity " + value);
    }

    private record GoldenCase(
            String name,
            String scenario,
            FindingCode code,
            FindingSeverity severity
    ) {
    }
}
