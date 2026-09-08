package dev.storyblock.validator;

import dev.storyblock.domain.*;
import dev.storyblock.renderer.DeterministicRenderer;
import java.util.List;
import java.util.Set;
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

  final DeterministicRenderer renderer;

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
    return DeterministicValidatorValidateOperationAction.validateOperation(this, base, actualHeadHash, operation);
  }

  public ValidationReport validateOperationCandidates(
      RevisionManifest base,
      String baseHash,
      EditOperation operation
  ) {
    return DeterministicValidatorValidateOperationCandidatesAction.validateOperationCandidates(this, base, baseHash, operation);
  }

  public ValidationReport validateRevision(
      RevisionManifest candidate,
      RevisionManifest base,
      String candidateHash
  ) {
    return DeterministicValidatorValidateRevisionAction.validateRevision(this, candidate, base, candidateHash);
  }

  public BlockValidation validateBlock(
      Ids.BlockId blockId,
      String text,
      BlockMetadata metadata,
      Set<String> presentBefore,
      BlockMetadata baselineMetadata
  ) {
    return DeterministicValidatorValidateBlockAction.validateBlock(this, blockId, text, metadata, presentBefore, baselineMetadata);
  }

  List<Candidate> candidates(
      RevisionManifest base,
      String baseHash,
      EditOperation operation
  ) {
    return DeterministicValidatorCandidatesAction.candidates(this, base, baseHash, operation);
  }

  List<Candidate> candidatesForRange(
      RevisionManifest base,
      String baseHash,
      Ids.SceneId sceneId,
      Ids.BlockId firstBlockId,
      List<BlockDraft> drafts
  ) {
    NarrativeScene scene = base.requireScene(sceneId);
    return candidatesAt(base, baseHash, sceneId, DeterministicValidatorIndexOf.indexOf(scene.blocks(), firstBlockId), drafts);
  }

  List<Candidate> candidatesAt(
      RevisionManifest base,
      String baseHash,
      Ids.SceneId sceneId,
      int insertionIndex,
      List<BlockDraft> drafts
  ) {
    return DeterministicValidatorCandidatesAtAction.candidatesAt(this, base, baseHash, sceneId, insertionIndex, drafts);
  }

  Set<String> presenceBefore(
      RevisionManifest base,
      String baseHash,
      NarrativeScene scene,
      int index
  ) {
    return DeterministicValidatorPresenceBeforeAction.presenceBefore(this, base, baseHash, scene, index);
  }

  public record BlockValidation(List<ValidationIssue> issues, Set<String> presentAfter) {
    public BlockValidation {
      issues = List.copyOf(issues);
      presentAfter = Set.copyOf(presentAfter);
    }
  }

  record Candidate(
      Ids.BlockId blockId,
      String text,
      BlockMetadata metadata,
      Set<String> presentBefore,
      BlockMetadata baselineMetadata
  ) {
  }
}
