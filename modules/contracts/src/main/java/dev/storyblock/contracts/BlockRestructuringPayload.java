package dev.storyblock.contracts;

import dev.storyblock.domain.EditOperation;
import java.util.Map;

import static dev.storyblock.contracts.EditOperationCanonicalMapperRange.range;

import static dev.storyblock.contracts.EditOperationCanonicalMapperDraft.draft;


import static dev.storyblock.contracts.EditOperationCanonicalMapperProvenance.provenance;



final class BlockRestructuringPayload {
  static Map<String, Object> value(EditOperation.SplitBlock split) {
    return Map.of(
          "block", range(split.block()),
          "split_after_grapheme", split.splitAfterGrapheme(),
          "new_blocks", split.newBlocks().stream()
              .map(EditOperationCanonicalMapperDraft::draft)
              .toList(),
          "provenance_mapping", provenance(split.provenanceMapping().sourceToResults())
      );
  }
  static Map<String, Object> value(EditOperation.MergeBlocks merge) {
    return Map.of(
          "range", range(merge.range()),
          "new_block", draft(merge.newBlock()),
          "provenance_mapping", provenance(merge.provenanceMapping().sourceToResults())
      );
  }
  static Map<String, Object> value(EditOperation.ExtendBlock extend) {
    return Map.of(
          "block", range(extend.block()),
          "position", extend.position().name().toLowerCase(java.util.Locale.ROOT),
          "replacement", draft(extend.replacement())
      );
  }
}
