package dev.storyblock.validator;

import dev.storyblock.domain.*;
import dev.storyblock.renderer.DeterministicRenderer;
import java.util.List;
import java.util.Set;

public final class DeterministicValidator {
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
    return OperationValidation.validateOperation(this, base, actualHeadHash, operation);
  }

  public ValidationReport validateOperationCandidates(
      RevisionManifest base,
      String baseHash,
      EditOperation operation
  ) {
    return OperationCandidateValidation.validateOperationCandidates(this, base, baseHash, operation);
  }

  public ValidationReport validateRevision(
      RevisionManifest candidate,
      RevisionManifest base,
      String candidateHash
  ) {
    return RevisionValidation.validateRevision(this, candidate, base, candidateHash);
  }

  public BlockValidation validateBlock(
      Ids.BlockId blockId,
      String text,
      BlockMetadata metadata,
      Set<String> presentBefore,
      BlockMetadata baselineMetadata
  ) {
    return BlockValidationAction.validateBlock(this, blockId, text, metadata, presentBefore, baselineMetadata);
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
    return InsertionCandidates.candidatesAt(this, base, baseHash, sceneId, insertionIndex, drafts);
  }

  Set<String> presenceBefore(
      RevisionManifest base,
      String baseHash,
      NarrativeScene scene,
      int index
  ) {
    return PriorPresence.presenceBefore(this, base, baseHash, scene, index);
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
