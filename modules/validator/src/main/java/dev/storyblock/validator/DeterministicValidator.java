package dev.storyblock.validator;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.EditInvariantException;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.EditOperationValidator;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.TextAnalysis;
import dev.storyblock.domain.UnicodeText;
import dev.storyblock.renderer.DeterministicRenderer;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.renderer.ResolvedBlockMetadata;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeterministicValidator {
    private static final Pattern ENTER_CUE = Pattern.compile(
            "走進|進入|闖入|踏入|來到|出現|\\b(?:enter(?:s|ed)?|arriv(?:e|es|ed)|walk(?:s|ed)? in)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern EXIT_CUE = Pattern.compile(
            "離開|走出|退出|離場|消失|\\b(?:exit(?:s|ed)?|lea(?:ve|ves|ft)|walk(?:s|ed)? out)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Set<String> OBSERVATION_FIELDS = Set.of(
            "time", "location", "weather", "pov"
    );

    private final DeterministicRenderer renderer;

    public DeterministicValidator() {
        this(new DeterministicRenderer());
    }

    public DeterministicValidator(DeterministicRenderer renderer) {
        this.renderer = java.util.Objects.requireNonNull(renderer, "renderer");
    }

    public ValidationReport validateOperation(
            RevisionManifest base,
            String actualHeadHash,
            EditOperation operation
    ) {
        try {
            EditOperationValidator.validate(base, actualHeadHash, operation);
            return ValidationReport.empty();
        } catch (EditInvariantException exception) {
            ValidationCode code = switch (exception.code()) {
                case REVISION_CONFLICT -> ValidationCode.REVISION_CONFLICT;
                case INVALID_BLOCK_ADJACENCY, BLOCK_VERSION_CONFLICT,
                        DUPLICATE_BLOCK_ID, INVALID_OPERATION -> ValidationCode.INVALID_BLOCK_ADJACENCY;
            };
            return errors(List.of(ValidationIssue.error(
                    code,
                    null,
                    exception.getMessage(),
                    Map.of(
                            "operation_id", operation.context().operationId().value(),
                            "operation_type", operation.type().canonicalName(),
                            "base_revision_id", base.id().value()
                    )
            )));
        } catch (IllegalArgumentException exception) {
            return errors(List.of(ValidationIssue.error(
                    ValidationCode.INVALID_BLOCK_ADJACENCY,
                    null,
                    exception.getMessage(),
                    Map.of(
                            "operation_id", operation.context().operationId().value(),
                            "operation_type", operation.type().canonicalName(),
                            "base_revision_id", base.id().value()
                    )
            )));
        }
    }

    public ValidationReport validateOperationCandidates(
            RevisionManifest base,
            String baseHash,
            EditOperation operation
    ) {
        List<Candidate> candidates = candidates(base, baseHash, operation);
        List<ValidationIssue> issues = new ArrayList<>();
        Set<String> present = candidates.isEmpty()
                ? Set.of()
                : new TreeSet<>(candidates.getFirst().presentBefore());
        for (Candidate candidate : candidates) {
            BlockValidation validation = validateBlock(
                    candidate.blockId(),
                    candidate.text(),
                    candidate.metadata(),
                    present,
                    candidate.baselineMetadata()
            );
            issues.addAll(validation.issues());
            present = validation.presentAfter();
        }
        return errors(issues);
    }

    public ValidationReport validateRevision(
            RevisionManifest candidate,
            RevisionManifest base,
            String candidateHash
    ) {
        RenderPacket packet = renderer.render(candidate, candidateHash, RenderRange.all());
        Map<Ids.BlockId, ResolvedBlockMetadata> resolved = new HashMap<>();
        for (ResolvedBlockMetadata metadata : packet.resolvedMetadata()) {
            resolved.put(metadata.blockId(), metadata);
        }
        Map<Ids.BlockId, NarrativeBlock> baseline = new HashMap<>();
        if (base != null) {
            for (NarrativeBlock block : base.liveBlocks()) {
                baseline.put(block.id(), block);
            }
        }

        List<ValidationIssue> issues = new ArrayList<>();
        for (NarrativeBlock block : candidate.liveBlocks()) {
            ResolvedBlockMetadata state = resolved.get(block.id());
            Set<String> presentBefore = strings(state.before().get("present_character_ids"));
            NarrativeBlock old = baseline.get(block.id());
            issues.addAll(validateBlock(
                    block.id(),
                    block.text(),
                    block.metadata(),
                    presentBefore,
                    old == null ? null : old.metadata()
            ).issues());
        }
        return errors(issues);
    }

    public BlockValidation validateBlock(
            Ids.BlockId blockId,
            String text,
            BlockMetadata metadata,
            Set<String> presentBefore,
            BlockMetadata baselineMetadata
    ) {
        List<ValidationIssue> issues = new ArrayList<>();
        TextAnalysis analysis = UnicodeText.analyze(text);
        validateText(blockId, analysis, issues);

        List<Map<String, Object>> events = maps(metadata.fields().get("presence_events"));
        validateEvidence(blockId, text, metadata.fields(), events, issues);
        Set<String> presentAfter = applyEvents(presentBefore, events);
        validateSpeakers(blockId, metadata.fields(), presentBefore, presentAfter, issues);
        validatePresenceCues(blockId, text, events, issues);
        validateUnknownHandling(blockId, text, metadata, baselineMetadata, issues);
        return new BlockValidation(List.copyOf(issues), Set.copyOf(presentAfter));
    }

    private static void validateText(
            Ids.BlockId blockId,
            TextAnalysis analysis,
            List<ValidationIssue> issues
    ) {
        if (analysis.graphemeCount() > UnicodeText.MAX_BLOCK_GRAPHEMES) {
            issues.add(ValidationIssue.error(
                    ValidationCode.BLOCK_TOO_LONG,
                    blockId,
                    "Block exceeds the maximum grapheme count",
                    Map.of(
                            "actual_graphemes", analysis.graphemeCount(),
                            "limit", UnicodeText.MAX_BLOCK_GRAPHEMES,
                            "safe_split_anchors", analysis.safeSplitAnchors(),
                            "normalization_version", analysis.normalizationVersion()
                    )
            ));
        }
        if (analysis.sentenceCount() < 1 || analysis.sentenceCount() > 2 || !analysis.complete()) {
            issues.add(ValidationIssue.error(
                    ValidationCode.INVALID_SENTENCE_COUNT,
                    blockId,
                    "Block must contain one or two complete sentences",
                    Map.of(
                            "actual_sentence_count", analysis.sentenceCount(),
                            "complete", analysis.complete(),
                            "required_min", 1,
                            "required_max", 2,
                            "safe_split_anchors", analysis.safeSplitAnchors(),
                            "parser_version", analysis.parserVersion()
                    )
            ));
        }
    }

    private static void validateSpeakers(
            Ids.BlockId blockId,
            Map<String, Object> metadata,
            Set<String> presentBefore,
            Set<String> presentAfter,
            List<ValidationIssue> issues
    ) {
        Set<String> speakers = directSpeakers(metadata.get("speech"));
        if (speakers.size() > 1) {
            issues.add(ValidationIssue.error(
                    ValidationCode.MULTIPLE_DIRECT_SPEAKERS,
                    blockId,
                    "A block can have at most one direct speaker",
                    Map.of("direct_speaker_ids", List.copyOf(new TreeSet<>(speakers)))
            ));
        }
        Set<String> available = new HashSet<>(presentBefore);
        available.addAll(presentAfter);
        for (String speaker : speakers) {
            if (!available.contains(speaker)) {
                issues.add(ValidationIssue.error(
                        ValidationCode.SPEAKER_NOT_PRESENT,
                        blockId,
                        "Direct speaker is not present in the resolved scene state",
                        Map.of(
                                "speaker_id", speaker,
                                "present_character_ids", List.copyOf(new TreeSet<>(available))
                        )
                ));
            }
        }
    }

    private static void validateEvidence(
            Ids.BlockId blockId,
            String text,
            Map<String, Object> metadata,
            List<Map<String, Object>> events,
            List<ValidationIssue> issues
    ) {
        for (int index = 0; index < events.size(); index++) {
            Map<String, Object> event = events.get(index);
            Object evidence = event.get("evidence");
            if (!(evidence instanceof Map<?, ?> span)) {
                issues.add(ValidationIssue.error(
                        ValidationCode.META_EVIDENCE_REQUIRED,
                        blockId,
                        "Presence events require same-block evidence",
                        Map.of("event_index", index, "event_type", String.valueOf(event.get("type")))
                ));
            } else if (!EvidenceSpans.matches(text, span)) {
                issues.add(ValidationIssue.error(
                        ValidationCode.META_EVIDENCE_REQUIRED,
                        blockId,
                        "Presence event evidence is not valid for the current block",
                        Map.of("event_index", index, "reason", "stale_or_invalid_span")
                ));
                issues.add(staleEvidence(blockId, "presence_events[" + index + "].evidence"));
            }
        }

        Object provenance = metadata.get("provenance");
        if (provenance instanceof Map<?, ?> provenanceMap) {
            Object evidence = provenanceMap.get("evidence");
            if (evidence instanceof List<?> spans) {
                for (int index = 0; index < spans.size(); index++) {
                    Object entry = spans.get(index);
                    if (!(entry instanceof Map<?, ?> span) || !EvidenceSpans.matches(text, span)) {
                        issues.add(staleEvidence(blockId, "provenance.evidence[" + index + "]"));
                    }
                }
            }
        }
    }

    private static ValidationIssue staleEvidence(Ids.BlockId blockId, String path) {
        return ValidationIssue.error(
                ValidationCode.EVIDENCE_SPAN_STALE,
                blockId,
                "Evidence span or quote hash does not match the current block text",
                Map.of("evidence_path", path)
        );
    }

    private static void validatePresenceCues(
            Ids.BlockId blockId,
            String text,
            List<Map<String, Object>> events,
            List<ValidationIssue> issues
    ) {
        requireEventForCue(blockId, text, events, "enter", ENTER_CUE, issues);
        requireEventForCue(blockId, text, events, "exit", EXIT_CUE, issues);
    }

    private static void requireEventForCue(
            Ids.BlockId blockId,
            String text,
            List<Map<String, Object>> events,
            String eventType,
            Pattern cuePattern,
            List<ValidationIssue> issues
    ) {
        Matcher matcher = cuePattern.matcher(text);
        if (!matcher.find()) {
            return;
        }
        boolean present = events.stream().anyMatch(event -> eventType.equals(event.get("type")));
        if (!present) {
            issues.add(ValidationIssue.error(
                    ValidationCode.PRESENCE_EVENT_REQUIRED,
                    blockId,
                    "Text contains an explicit presence transition without matching metadata",
                    Map.of(
                            "required_event_type", eventType,
                            "matched_cue", matcher.group(),
                            "cue_start_utf16", matcher.start(),
                            "rule_version", ValidatorModule.VERSION
                    )
            ));
        }
    }

    private static void validateUnknownHandling(
            Ids.BlockId blockId,
            String text,
            BlockMetadata candidate,
            BlockMetadata baseline,
            List<ValidationIssue> issues
    ) {
        for (String field : OBSERVATION_FIELDS) {
            Object candidateValue = candidate.fields().get(field);
            if (!isMode(candidateValue, "explicit")) {
                continue;
            }
            boolean wasUnknown = baseline != null && isMode(baseline.fields().get(field), "unknown");
            boolean extractorAuthored = isExtractorAuthored(candidate.fields().get("provenance"));
            if ((wasUnknown || extractorAuthored) && !hasEvidence(candidateValue, text)
                    && !hasProvenanceEvidence(candidate.fields().get("provenance"), text)) {
                issues.add(ValidationIssue.error(
                        ValidationCode.UNKNOWN_META_VALUE_INVENTED,
                        blockId,
                        "Unknown metadata cannot become explicit without evidence",
                        Map.of(
                                "field", field,
                                "previous_mode", wasUnknown ? "unknown" : "not_present",
                                "candidate_mode", "explicit"
                        )
                ));
            }
        }
    }

    private List<Candidate> candidates(
            RevisionManifest base,
            String baseHash,
            EditOperation operation
    ) {
        return switch (operation) {
            case EditOperation.InsertBlocks insert -> candidatesAt(
                    base,
                    baseHash,
                    insert.insertionPoint().sceneId(),
                    EditOperationValidator.insertionIndex(
                            base.requireScene(insert.insertionPoint().sceneId()), insert.insertionPoint()
                    ),
                    insert.blocks()
            );
            case EditOperation.ReplaceBlockRange replace -> candidatesForRange(
                    base, baseHash, replace.range().sceneId(), replace.range().firstBlockId(), replace.newBlocks()
            );
            case EditOperation.SplitBlock split -> candidatesForRange(
                    base, baseHash, split.block().sceneId(), split.block().firstBlockId(), split.newBlocks()
            );
            case EditOperation.MergeBlocks merge -> candidatesForRange(
                    base, baseHash, merge.range().sceneId(), merge.range().firstBlockId(), List.of(merge.newBlock())
            );
            case EditOperation.ExtendBlock extend -> candidatesForRange(
                    base, baseHash, extend.block().sceneId(), extend.block().firstBlockId(),
                    List.of(extend.replacement())
            );
            case EditOperation.CorrectBlockMeta correction -> {
                NarrativeBlock block = base.requireBlock(correction.block().blockId());
                NarrativeScene scene = base.requireScene(correction.sceneId());
                int index = indexOf(scene.blocks(), block.id());
                yield candidatesAt(
                        base,
                        baseHash,
                        scene.id(),
                        index,
                        List.of(new BlockDraft(
                                block.id(), block.text(), correction.correctedMetadata(), block.extensions()
                        ))
                );
            }
            case EditOperation.DeleteBlockRange ignored -> List.of();
            case EditOperation.MoveBlockRange ignored -> List.of();
            case EditOperation.SetSceneInitialMeta ignored -> List.of();
            case EditOperation.RestoreRevisionContent ignored -> List.of();
        };
    }

    private List<Candidate> candidatesForRange(
            RevisionManifest base,
            String baseHash,
            Ids.SceneId sceneId,
            Ids.BlockId firstBlockId,
            List<BlockDraft> drafts
    ) {
        NarrativeScene scene = base.requireScene(sceneId);
        return candidatesAt(base, baseHash, sceneId, indexOf(scene.blocks(), firstBlockId), drafts);
    }

    private List<Candidate> candidatesAt(
            RevisionManifest base,
            String baseHash,
            Ids.SceneId sceneId,
            int insertionIndex,
            List<BlockDraft> drafts
    ) {
        NarrativeScene scene = base.requireScene(sceneId);
        Set<String> present = presenceBefore(base, baseHash, scene, insertionIndex);
        Map<Ids.BlockId, NarrativeBlock> current = new HashMap<>();
        for (NarrativeBlock block : base.liveBlocks()) {
            current.put(block.id(), block);
        }
        List<Candidate> result = new ArrayList<>();
        for (BlockDraft draft : drafts) {
            NarrativeBlock old = current.get(draft.id());
            result.add(new Candidate(
                    draft.id(),
                    draft.text(),
                    draft.metadata(),
                    Set.copyOf(present),
                    old == null ? null : old.metadata()
            ));
            present = applyEvents(present, maps(draft.metadata().fields().get("presence_events")));
        }
        return List.copyOf(result);
    }

    private Set<String> presenceBefore(
            RevisionManifest base,
            String baseHash,
            NarrativeScene scene,
            int index
    ) {
        if (index == 0) {
            return scene.initialMeta() == null
                    ? Set.of()
                    : strings(scene.initialMeta().fields().get("present_character_ids"));
        }
        Ids.BlockId previous = scene.blocks().get(index - 1).id();
        RenderPacket packet = renderer.render(base, baseHash, RenderRange.all());
        for (ResolvedBlockMetadata metadata : packet.resolvedMetadata()) {
            if (metadata.blockId().equals(previous)) {
                return strings(metadata.after().get("present_character_ids"));
            }
        }
        throw new IllegalArgumentException("Renderer omitted preceding block " + previous.value());
    }

    private static Set<String> directSpeakers(Object speechValue) {
        if (!(speechValue instanceof Map<?, ?> speech)) {
            return Set.of();
        }
        Set<String> speakers = new HashSet<>();
        addStrings(speakers, speech.get("direct_speaker_id"));
        addStrings(speakers, speech.get("direct_speaker_ids"));
        Object turns = speech.get("turns");
        if (turns instanceof List<?> values) {
            for (Object value : values) {
                if (value instanceof Map<?, ?> turn
                        && "direct".equals(turn.get("channel"))) {
                    addStrings(speakers, turn.get("speaker_id"));
                }
            }
        }
        return Set.copyOf(speakers);
    }

    private static void addStrings(Set<String> destination, Object value) {
        if (value instanceof String string && !string.isBlank()) {
            destination.add(string);
        } else if (value instanceof Collection<?> values) {
            for (Object entry : values) {
                if (entry instanceof String string && !string.isBlank()) {
                    destination.add(string);
                }
            }
        }
    }

    private static Set<String> applyEvents(
            Set<String> presentBefore,
            List<Map<String, Object>> events
    ) {
        Set<String> present = new TreeSet<>(presentBefore);
        for (Map<String, Object> event : events) {
            Object character = event.get("character_id");
            if (!(character instanceof String characterId)) {
                continue;
            }
            if ("enter".equals(event.get("type"))) {
                present.add(characterId);
            } else if ("exit".equals(event.get("type"))) {
                present.remove(characterId);
            }
        }
        return Set.copyOf(present);
    }

    private static boolean isMode(Object value, String expectedMode) {
        return value instanceof Map<?, ?> map && expectedMode.equals(map.get("mode"));
    }

    private static boolean hasEvidence(Object value, String text) {
        return value instanceof Map<?, ?> map
                && map.get("evidence") instanceof Map<?, ?> evidence
                && EvidenceSpans.matches(text, evidence);
    }

    private static boolean hasProvenanceEvidence(Object value, String text) {
        return value instanceof Map<?, ?> map
                && map.get("evidence") instanceof List<?> evidence
                && evidence.stream().anyMatch(entry -> entry instanceof Map<?, ?> span
                        && EvidenceSpans.matches(text, span));
    }

    private static boolean isExtractorAuthored(Object value) {
        if (!(value instanceof Map<?, ?> map) || !(map.get("source") instanceof String source)) {
            return false;
        }
        return Set.of("extractor", "llm", "model").contains(source.toLowerCase(Locale.ROOT));
    }

    private static List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof Map<?, ?> raw) {
                Map<String, Object> typed = new LinkedHashMap<>();
                for (Map.Entry<?, ?> field : raw.entrySet()) {
                    if (field.getKey() instanceof String key) {
                        typed.put(key, field.getValue());
                    }
                }
                maps.add(java.util.Collections.unmodifiableMap(typed));
            }
        }
        return List.copyOf(maps);
    }

    private static Set<String> strings(Object value) {
        Set<String> strings = new TreeSet<>();
        addStrings(strings, value);
        return Set.copyOf(strings);
    }

    private static int indexOf(List<NarrativeBlock> blocks, Ids.BlockId blockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).id().equals(blockId)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Scene does not contain block " + blockId.value());
    }

    private static ValidationReport errors(List<ValidationIssue> issues) {
        return new ValidationReport(issues, List.of());
    }

    public record BlockValidation(List<ValidationIssue> issues, Set<String> presentAfter) {
        public BlockValidation {
            issues = List.copyOf(issues);
            presentAfter = Set.copyOf(presentAfter);
        }
    }

    private record Candidate(
            Ids.BlockId blockId,
            String text,
            BlockMetadata metadata,
            Set<String> presentBefore,
            BlockMetadata baselineMetadata
    ) {
    }
}
