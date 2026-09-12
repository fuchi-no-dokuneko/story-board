package dev.storyblock.domain;

import java.util.Objects;
import static dev.storyblock.domain.EditOperation.*;

public interface SceneMovementOperations {
  record MoveBlockRange(
      EditContext context,
      BlockRangeGuard range,
      InsertionPoint destination,
      SceneBoundaryContract expectedSourceBoundary,
      SceneBoundaryContract expectedDestinationBoundary
  ) implements EditOperation {
    public MoveBlockRange {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(range, "range");
      Objects.requireNonNull(destination, "destination");
      Objects.requireNonNull(expectedSourceBoundary, "expectedSourceBoundary");
      Objects.requireNonNull(expectedDestinationBoundary, "expectedDestinationBoundary");
      if (!expectedSourceBoundary.sceneId().equals(range.sceneId())) {
        throw new IllegalArgumentException("Source boundary must describe the guarded scene");
      }
      if (!expectedDestinationBoundary.sceneId().equals(destination.sceneId())) {
        throw new IllegalArgumentException("Destination boundary must describe the destination scene");
      }
    }

    @Override
    public Type type() {
      return Type.MOVE_BLOCK_RANGE;
    }
  }

  record CorrectBlockMeta(
      EditContext context,
      Ids.SceneId sceneId,
      BlockVersionRef block,
      BlockMetadata correctedMetadata
  ) implements EditOperation {
    public CorrectBlockMeta {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(sceneId, "sceneId");
      Objects.requireNonNull(block, "block");
      Objects.requireNonNull(correctedMetadata, "correctedMetadata");
    }

    @Override
    public Type type() {
      return Type.CORRECT_BLOCK_META;
    }
  }
}
