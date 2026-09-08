package dev.storyblock.domain;

import java.util.Objects;
import java.util.regex.Pattern;
import static dev.storyblock.domain.EditOperation.*;

public interface SceneStateOperations {
  record SetSceneInitialMeta(
      EditContext context,
      Ids.SceneId sceneId,
      SceneBoundaryContract expectedBoundary,
      SceneSeed initialMeta
  ) implements EditOperation {
    public SetSceneInitialMeta {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(sceneId, "sceneId");
      Objects.requireNonNull(expectedBoundary, "expectedBoundary");
      Objects.requireNonNull(initialMeta, "initialMeta");
      if (!sceneId.equals(expectedBoundary.sceneId())) {
        throw new IllegalArgumentException("Scene seed boundary must describe the target scene");
      }
    }

    @Override
    public Type type() {
      return Type.SET_SCENE_INITIAL_META;
    }
  }

  record RestoreRevisionContent(
      EditContext context,
      Ids.RevisionId restoreRevisionId,
      String expectedRestoreHash
  ) implements EditOperation {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public RestoreRevisionContent {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(restoreRevisionId, "restoreRevisionId");
      if (expectedRestoreHash == null || !SHA_256.matcher(expectedRestoreHash).matches()) {
        throw new IllegalArgumentException("Expected restore hash must be lowercase SHA-256");
      }
    }

    @Override
    public Type type() {
      return Type.RESTORE_REVISION_CONTENT;
    }
  }
}
