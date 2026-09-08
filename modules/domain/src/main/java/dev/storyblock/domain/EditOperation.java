package dev.storyblock.domain;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public sealed interface EditOperation extends BlockInsertionOperations, BlockRangeOperations, BlockStructureOperations, SceneMovementOperations, SceneStateOperations permits
    EditOperation.InsertBlocks,
    EditOperation.ReplaceBlockRange,
    EditOperation.DeleteBlockRange,
    EditOperation.SplitBlock,
    EditOperation.MergeBlocks,
    EditOperation.ExtendBlock,
    EditOperation.MoveBlockRange,
    EditOperation.CorrectBlockMeta,
    EditOperation.SetSceneInitialMeta,
    EditOperation.RestoreRevisionContent {

  enum Type {
    INSERT_BLOCKS("insert_blocks"),
    REPLACE_BLOCK_RANGE("replace_block_range"),
    DELETE_BLOCK_RANGE("delete_block_range"),
    SPLIT_BLOCK("split_block"),
    MERGE_BLOCKS("merge_blocks"),
    EXTEND_BLOCK("extend_block"),
    MOVE_BLOCK_RANGE("move_block_range"),
    CORRECT_BLOCK_META("correct_block_meta"),
    SET_SCENE_INITIAL_META("set_scene_initial_meta"),
    RESTORE_REVISION_CONTENT("restore_revision_content");

    private final String canonicalName;

    Type(String canonicalName) {
      this.canonicalName = canonicalName;
    }

    public String canonicalName() {
      return canonicalName;
    }
  }

  enum ExtensionPosition {
    BEFORE,
    AFTER
  }

  EditContext context();

  Type type();

}
