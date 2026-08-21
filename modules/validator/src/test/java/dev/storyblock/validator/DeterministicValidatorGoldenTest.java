package dev.storyblock.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.BlockSequenceHash;
import dev.storyblock.domain.BlockVersionRef;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.domain.UnicodeText;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DeterministicValidatorGoldenTest {
    private final DeterministicValidator validator = new DeterministicValidator();

    @Test
    void everyDocumentedCodeHasPassingAndFailingGoldenCases() throws IOException {
        List<GoldenCase> cases = loadCases();
        Map<ValidationCode, Set<Boolean>> outcomes = new EnumMap<>(ValidationCode.class);

        for (GoldenCase golden : cases) {
            ValidationReport report = execute(golden.scenario());
            boolean found = report.violations().stream()
                    .anyMatch(issue -> issue.code() == golden.code());
            assertEquals(golden.expectedViolation(), found, golden.name());
            outcomes.computeIfAbsent(golden.code(), ignored -> new java.util.HashSet<>())
                    .add(golden.expectedViolation());
        }

        assertEquals(Set.of(ValidationCode.values()), outcomes.keySet());
        outcomes.forEach((code, values) -> assertEquals(Set.of(false, true), values, code.name()));
    }

    @Test
    void countAndEvidenceViolationsCarryStableDiagnosticDetails() {
        DeterministicValidator.BlockValidation tooLong = validateBlock(
                "字".repeat(100) + "。", BlockMetadata.empty(), Set.of(), null
        );
        ValidationIssue length = issue(tooLong, ValidationCode.BLOCK_TOO_LONG);
        assertEquals(101, length.details().get("actual_graphemes"));
        assertEquals(100, length.details().get("limit"));
        assertTrue(length.details().containsKey("safe_split_anchors"));

        DeterministicValidator.BlockValidation stale = validateBlock(
                "阿明進入房間。",
                new BlockMetadata(Map.of(
                        "presence_events", List.of(event("enter", false))
                )),
                Set.of(),
                null
        );
        ValidationIssue evidence = issue(stale, ValidationCode.EVIDENCE_SPAN_STALE);
        assertEquals("presence_events[0].evidence", evidence.details().get("evidence_path"));
    }

    private ValidationReport execute(String scenario) {
        return switch (scenario) {
            case "valid_block" -> report(validateBlock(
                    "完整一句。", BlockMetadata.empty(), Set.of(), null
            ));
            case "overlong_block" -> report(validateBlock(
                    "字".repeat(100) + "。", BlockMetadata.empty(), Set.of(), null
            ));
            case "incomplete_block" -> report(validateBlock(
                    "沒有完整句號", BlockMetadata.empty(), Set.of(), null
            ));
            case "single_speaker", "present_speaker" -> report(validateBlock(
                    "阿明說話。",
                    new BlockMetadata(Map.of(
                            "speech", Map.of("direct_speaker_id", "char_ming")
                    )),
                    Set.of("char_ming"),
                    null
            ));
            case "multiple_speakers" -> report(validateBlock(
                    "兩人交談。",
                    new BlockMetadata(Map.of(
                            "speech", Map.of(
                                    "direct_speaker_ids", List.of("char_ming", "char_kei")
                            )
                    )),
                    Set.of("char_ming", "char_kei"),
                    null
            ));
            case "absent_speaker" -> report(validateBlock(
                    "阿明說話。",
                    new BlockMetadata(Map.of(
                            "speech", Map.of("direct_speaker_id", "char_ming")
                    )),
                    Set.of("char_kei"),
                    null
            ));
            case "valid_presence_evidence" -> report(validateBlock(
                    "阿明進入房間。",
                    new BlockMetadata(Map.of(
                            "presence_events", List.of(event("enter", true))
                    )),
                    Set.of(),
                    null
            ));
            case "missing_presence_evidence" -> report(validateBlock(
                    "阿明進入房間。",
                    new BlockMetadata(Map.of(
                            "presence_events", List.of(Map.of(
                                    "type", "enter", "character_id", "char_ming"
                            ))
                    )),
                    Set.of(),
                    null
            ));
            case "missing_presence_event" -> report(validateBlock(
                    "阿明進入房間。", BlockMetadata.empty(), Set.of(), null
            ));
            case "current_evidence" -> report(validateBlock(
                    "完整一句。",
                    new BlockMetadata(Map.of(
                            "provenance", Map.of("evidence", List.of(evidence("完整一句。", true)))
                    )),
                    Set.of(),
                    null
            ));
            case "stale_evidence" -> report(validateBlock(
                    "完整一句。",
                    new BlockMetadata(Map.of(
                            "provenance", Map.of("evidence", List.of(evidence("完整一句。", false)))
                    )),
                    Set.of(),
                    null
            ));
            case "evidenced_unknown_upgrade" -> report(validateBlock(
                    "完整一句。",
                    new BlockMetadata(Map.of(
                            "weather", Map.of(
                                    "mode", "explicit",
                                    "value", "rain",
                                    "evidence", evidence("完整一句。", true)
                            )
                    )),
                    Set.of(),
                    new BlockMetadata(Map.of("weather", Map.of("mode", "unknown")))
            ));
            case "invented_unknown_upgrade" -> report(validateBlock(
                    "完整一句。",
                    new BlockMetadata(Map.of(
                            "weather", Map.of("mode", "explicit", "value", "rain")
                    )),
                    Set.of(),
                    new BlockMetadata(Map.of("weather", Map.of("mode", "unknown")))
            ));
            case "adjacent_range" -> operationReport(false, false);
            case "non_adjacent_range" -> operationReport(true, false);
            case "matching_revision" -> operationReport(false, false);
            case "stale_revision" -> operationReport(false, true);
            default -> throw new IllegalArgumentException("Unknown golden scenario " + scenario);
        };
    }

    private ValidationReport operationReport(boolean nonAdjacent, boolean staleRevision) {
        RevisionManifest base = revision();
        NarrativeScene scene = base.novel().chapters().getFirst().scenes().getFirst();
        String baseHash = NarrativeCanonicalMapper.toCanonical(base).contentHash();
        BlockRangeGuard guard;
        if (nonAdjacent) {
            List<BlockVersionRef> references = List.of(
                    BlockVersionRef.from(scene.blocks().getFirst()),
                    BlockVersionRef.from(scene.blocks().getLast())
            );
            guard = new BlockRangeGuard(
                    scene.id(), references, null, null, BlockSequenceHash.ofReferences(references)
            );
        } else {
            guard = BlockRangeGuard.capture(
                    scene, scene.blocks().getFirst().id(), scene.blocks().get(1).id()
            );
        }
        EditContext context = new EditContext(
                Ids.OperationId.create(),
                "golden-operation",
                base.novel().id(),
                base.id(),
                staleRevision ? "sha256:" + "0".repeat(64) : baseHash
        );
        return validator.validateOperation(
                base,
                baseHash,
                new EditOperation.DeleteBlockRange(context, guard)
        );
    }

    private DeterministicValidator.BlockValidation validateBlock(
            String text,
            BlockMetadata metadata,
            Set<String> present,
            BlockMetadata baseline
    ) {
        return validator.validateBlock(Ids.BlockId.create(), text, metadata, present, baseline);
    }

    private static ValidationReport report(DeterministicValidator.BlockValidation validation) {
        return new ValidationReport(validation.issues(), List.of());
    }

    private static ValidationIssue issue(
            DeterministicValidator.BlockValidation validation,
            ValidationCode code
    ) {
        return validation.issues().stream()
                .filter(candidate -> candidate.code() == code)
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, Object> event(String type, boolean currentEvidence) {
        return Map.of(
                "type", type,
                "character_id", "char_ming",
                "evidence", evidence("阿明進入房間。", currentEvidence)
        );
    }

    private static Map<String, Object> evidence(String text, boolean current) {
        return Map.of(
                "start_grapheme", 0,
                "end_grapheme", UnicodeText.graphemeCount(text),
                "quote", text,
                "quote_hash", current ? EvidenceSpans.quoteHash(text) : "sha256:" + "0".repeat(64)
        );
    }

    private static RevisionManifest revision() {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        List<NarrativeBlock> blocks = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            blocks.add(NarrativeBlock.create(
                    Ids.BlockId.create(),
                    OrderKey.rebalanced(index, 3),
                    "第" + (index + 1) + "句。",
                    BlockMetadata.empty(),
                    Map.of()
            ));
        }
        NarrativeScene scene = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.initial(),
                "Golden",
                TransitionMode.OPENING,
                SceneSeed.empty(),
                blocks,
                Map.of()
        );
        NarrativeChapter chapter = new NarrativeChapter(
                chapterId, OrderKey.initial(), "Chapter", List.of(scene), Map.of()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                Instant.parse("2026-08-21T12:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(), List.of(chapter), Map.of())
        );
    }

    @SuppressWarnings("unchecked")
    private static List<GoldenCase> loadCases() throws IOException {
        try (InputStream input = DeterministicValidatorGoldenTest.class
                .getResourceAsStream("/golden/validator-cases.json")) {
            assertFalse(input == null, "Missing validator golden cases");
            List<Map<String, Object>> values = CanonicalJson.mapper().readValue(input, List.class);
            return values.stream()
                    .map(value -> new GoldenCase(
                            (String) value.get("name"),
                            ValidationCode.valueOf((String) value.get("code")),
                            (String) value.get("scenario"),
                            (Boolean) value.get("expected_violation")
                    ))
                    .toList();
        }
    }

    private record GoldenCase(
            String name,
            ValidationCode code,
            String scenario,
            boolean expectedViolation
    ) {
    }
}
