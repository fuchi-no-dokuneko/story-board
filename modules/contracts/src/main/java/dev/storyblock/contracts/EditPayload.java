package dev.storyblock.contracts;

import dev.storyblock.domain.EditOperation;
import java.util.Map;

import static dev.storyblock.contracts.EditOperationCanonicalMapperRange.range;

import static dev.storyblock.contracts.EditOperationCanonicalMapperDraft.draft;

import static dev.storyblock.contracts.EditOperationCanonicalMapperInsertionPoint.insertionPoint;


import static dev.storyblock.contracts.EditOperationCanonicalMapperBoundary.boundary;

import static dev.storyblock.contracts.EditOperationCanonicalMapperBlockReference.blockReference;

final class EditPayload {
  static Map<String, Object> payload(EditOperation operation)  {
    return switch (operation) {
      case EditOperation.InsertBlocks insert -> Map.of(
          "insertion_point", insertionPoint(insert.insertionPoint()),
          "blocks", insert.blocks().stream()
              .map(EditOperationCanonicalMapperDraft::draft)
              .toList()
      );
      case EditOperation.ReplaceBlockRange replace -> Map.of(
          "range", range(replace.range()),
          "new_blocks", replace.newBlocks().stream()
              .map(EditOperationCanonicalMapperDraft::draft)
              .toList()
      );
      case EditOperation.DeleteBlockRange delete -> Map.of("range", range(delete.range()));
      case EditOperation.SplitBlock split -> BlockRestructuringPayload.value(split);
      case EditOperation.MergeBlocks merge -> BlockRestructuringPayload.value(merge);
      case EditOperation.ExtendBlock extend -> BlockRestructuringPayload.value(extend);
      case EditOperation.MoveBlockRange move -> Map.of(
          "range", range(move.range()),
          "destination", insertionPoint(move.destination()),
          "expected_source_boundary", boundary(move.expectedSourceBoundary()),
          "expected_destination_boundary", boundary(move.expectedDestinationBoundary())
      );
      case EditOperation.CorrectBlockMeta correction -> Map.of(
          "scene_id", correction.sceneId().value(),
          "block", blockReference(correction.block()),
          "corrected_meta", correction.correctedMetadata().fields()
      );
      case EditOperation.SetSceneInitialMeta sceneSeed -> Map.of(
          "scene_id", sceneSeed.sceneId().value(),
          "expected_boundary", boundary(sceneSeed.expectedBoundary()),
          "initial_meta", sceneSeed.initialMeta().fields()
      );
      case EditOperation.RestoreRevisionContent restore -> Map.of(
          "restore_revision_id", restore.restoreRevisionId().value(),
          "expected_restore_hash", restore.expectedRestoreHash()
      );
    };
  }
}
