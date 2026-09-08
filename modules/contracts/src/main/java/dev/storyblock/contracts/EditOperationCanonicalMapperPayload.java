package dev.storyblock.contracts;

import dev.storyblock.domain.EditOperation;
import java.util.Map;

final class EditOperationCanonicalMapperPayload {
  static Map<String, Object> payload(EditOperation operation)  {
    return switch (operation) {
      case EditOperation.InsertBlocks insert -> Map.of(
          "insertion_point", EditOperationCanonicalMapperInsertionPoint.insertionPoint(insert.insertionPoint()),
          "blocks", insert.blocks().stream()
              .map(EditOperationCanonicalMapperDraft::draft)
              .toList()
      );
      case EditOperation.ReplaceBlockRange replace -> Map.of(
          "range", EditOperationCanonicalMapperRange.range(replace.range()),
          "new_blocks", replace.newBlocks().stream()
              .map(EditOperationCanonicalMapperDraft::draft)
              .toList()
      );
      case EditOperation.DeleteBlockRange delete -> Map.of("range", EditOperationCanonicalMapperRange.range(delete.range()));
      case EditOperation.SplitBlock split -> Map.of(
          "block", EditOperationCanonicalMapperRange.range(split.block()),
          "split_after_grapheme", split.splitAfterGrapheme(),
          "new_blocks", split.newBlocks().stream()
              .map(EditOperationCanonicalMapperDraft::draft)
              .toList(),
          "provenance_mapping", EditOperationCanonicalMapperProvenance.provenance(split.provenanceMapping().sourceToResults())
      );
      case EditOperation.MergeBlocks merge -> Map.of(
          "range", EditOperationCanonicalMapperRange.range(merge.range()),
          "new_block", EditOperationCanonicalMapperDraft.draft(merge.newBlock()),
          "provenance_mapping", EditOperationCanonicalMapperProvenance.provenance(merge.provenanceMapping().sourceToResults())
      );
      case EditOperation.ExtendBlock extend -> Map.of(
          "block", EditOperationCanonicalMapperRange.range(extend.block()),
          "position", extend.position().name().toLowerCase(java.util.Locale.ROOT),
          "replacement", EditOperationCanonicalMapperDraft.draft(extend.replacement())
      );
      case EditOperation.MoveBlockRange move -> Map.of(
          "range", EditOperationCanonicalMapperRange.range(move.range()),
          "destination", EditOperationCanonicalMapperInsertionPoint.insertionPoint(move.destination()),
          "expected_source_boundary", EditOperationCanonicalMapperBoundary.boundary(move.expectedSourceBoundary()),
          "expected_destination_boundary", EditOperationCanonicalMapperBoundary.boundary(move.expectedDestinationBoundary())
      );
      case EditOperation.CorrectBlockMeta correction -> Map.of(
          "scene_id", correction.sceneId().value(),
          "block", EditOperationCanonicalMapperBlockReference.blockReference(correction.block()),
          "corrected_meta", correction.correctedMetadata().fields()
      );
      case EditOperation.SetSceneInitialMeta sceneSeed -> Map.of(
          "scene_id", sceneSeed.sceneId().value(),
          "expected_boundary", EditOperationCanonicalMapperBoundary.boundary(sceneSeed.expectedBoundary()),
          "initial_meta", sceneSeed.initialMeta().fields()
      );
      case EditOperation.RestoreRevisionContent restore -> Map.of(
          "restore_revision_id", restore.restoreRevisionId().value(),
          "expected_restore_hash", restore.expectedRestoreHash()
      );
    };
  }
}
