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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class DeterministicValidator {
    static final Pattern ENTER_CUE = Pattern.compile(
            "走進|進入|闖入|踏入|來到|出現|\\b(?:enter(?:s|ed)?|arriv(?:e|es|ed)|walk(?:s|ed)? in)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    static final Pattern EXIT_CUE = Pattern.compile(
            "離開|走出|退出|離場|消失|\\b(?:exit(?:s|ed)?|lea(?:ve|ves|ft)|walk(?:s|ed)? out)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    static final Set<String> OBSERVATION_FIELDS = Set.of(
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
            return DeterministicValidatorErrors.errors(List.of(ValidationIssue.error(
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
            return DeterministicValidatorErrors.errors(List.of(ValidationIssue.error(
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
        return DeterministicValidatorErrors.errors(issues);
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
            Set<String> presentBefore = DeterministicValidatorStrings.strings(state.before().get("present_character_ids"));
            NarrativeBlock old = baseline.get(block.id());
            issues.addAll(validateBlock(
                    block.id(),
                    block.text(),
                    block.metadata(),
                    presentBefore,
                    old == null ? null : old.metadata()
            ).issues());
        }
        return DeterministicValidatorErrors.errors(issues);
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
        DeterministicValidatorValidateText.validateText(blockId, analysis, issues);

        List<Map<String, Object>> events = DeterministicValidatorMaps.maps(metadata.fields().get("presence_events"));
        DeterministicValidatorValidateEvidence.validateEvidence(blockId, text, metadata.fields(), events, issues);
        Set<String> presentAfter = DeterministicValidatorApplyEvents.applyEvents(presentBefore, events);
        DeterministicValidatorValidateSpeakers.validateSpeakers(blockId, metadata.fields(), presentBefore, presentAfter, issues);
        DeterministicValidatorValidatePresenceCues.validatePresenceCues(blockId, text, events, issues);
        DeterministicValidatorValidateUnknownHandling.validateUnknownHandling(blockId, text, metadata, baselineMetadata, issues);
        return new BlockValidation(List.copyOf(issues), Set.copyOf(presentAfter));
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
                int index = DeterministicValidatorIndexOf.indexOf(scene.blocks(), block.id());
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
        return candidatesAt(base, baseHash, sceneId, DeterministicValidatorIndexOf.indexOf(scene.blocks(), firstBlockId), drafts);
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
            present = DeterministicValidatorApplyEvents.applyEvents(present, DeterministicValidatorMaps.maps(draft.metadata().fields().get("presence_events")));
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
                    : DeterministicValidatorStrings.strings(scene.initialMeta().fields().get("present_character_ids"));
        }
        Ids.BlockId previous = scene.blocks().get(index - 1).id();
        RenderPacket packet = renderer.render(base, baseHash, RenderRange.all());
        for (ResolvedBlockMetadata metadata : packet.resolvedMetadata()) {
            if (metadata.blockId().equals(previous)) {
                return DeterministicValidatorStrings.strings(metadata.after().get("present_character_ids"));
            }
        }
        throw new IllegalArgumentException("Renderer omitted preceding block " + previous.value());
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
