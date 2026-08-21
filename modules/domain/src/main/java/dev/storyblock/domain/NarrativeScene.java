package dev.storyblock.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record NarrativeScene(
        Ids.SceneId id,
        Ids.ChapterId chapterId,
        OrderKey orderKey,
        String title,
        TransitionMode transitionMode,
        SceneSeed initialMeta,
        List<NarrativeBlock> blocks,
        Map<String, Object> extensions
) {
    public NarrativeScene {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(chapterId, "chapterId");
        Objects.requireNonNull(orderKey, "orderKey");
        Objects.requireNonNull(transitionMode, "transitionMode");
        blocks = List.copyOf(blocks);
        extensions = CanonicalValues.freezeMap(extensions, "scene.extensions");
        validateOrderedBlocks(blocks);
    }

    public NarrativeScene withBlocks(List<NarrativeBlock> newBlocks) {
        return new NarrativeScene(
                id, chapterId, orderKey, title, transitionMode, initialMeta, newBlocks, extensions
        );
    }

    public NarrativeScene withInitialMeta(SceneSeed newInitialMeta) {
        return new NarrativeScene(
                id, chapterId, orderKey, title, transitionMode, newInitialMeta, blocks, extensions
        );
    }

    private static void validateOrderedBlocks(List<NarrativeBlock> blocks) {
        Set<Ids.BlockId> ids = new HashSet<>();
        Set<Ids.BlockVersionId> versions = new HashSet<>();
        OrderKey previous = null;
        for (NarrativeBlock block : blocks) {
            Objects.requireNonNull(block, "scene block");
            if (previous != null && previous.compareTo(block.orderKey()) >= 0) {
                throw new IllegalArgumentException("Scene block order keys must be strictly increasing");
            }
            if (!ids.add(block.id())) {
                throw new IllegalArgumentException("Scene contains duplicate block ID " + block.id().value());
            }
            if (!versions.add(block.versionId())) {
                throw new IllegalArgumentException(
                        "Scene contains duplicate block version ID " + block.versionId().value()
                );
            }
            previous = block.orderKey();
        }
    }
}
