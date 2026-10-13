package dev.storyblock.domain;

import java.util.List;
import java.util.Optional;

/** The prose projection; storage and editing retain the original sequences. */
public final class NarrativeView {
    private NarrativeView() { }

    public static List<NarrativeBlock> visibleBlocks(NarrativeScene scene) {
        return visibleBlocks(scene.blocks());
    }

    public static List<NarrativeBlock> visibleBlocks(RevisionManifest revision) {
        return revision.novel().chapters().stream().flatMap(chapter -> chapter.scenes().stream())
                .flatMap(scene -> visibleBlocks(scene).stream()).toList();
    }

    public static <T extends NarrativeText> List<T> visibleBlocks(List<T> blocks) {
        return blocks.stream().filter(block -> !BlockHidden.isHidden(block.extensions())).toList();
    }

    public static Optional<NarrativeBlock> previousVisible(NarrativeScene scene, int index) {
        if (index < 0 || index > scene.blocks().size()) {
            throw new IllegalArgumentException("Block index is outside its scene");
        }
        for (int i = index - 1; i >= 0; i--) {
            NarrativeBlock block = scene.blocks().get(i);
            if (!block.isHidden()) return Optional.of(block);
        }
        return Optional.empty();
    }

    public static void requireRenderable(RevisionManifest revision, Ids.BlockId blockId) {
        if (blockId != null && revision.requireBlock(blockId).isHidden()) {
            throw new HiddenBlockException(HiddenBlockException.Code.HIDDEN_BLOCK_NOT_RENDERABLE,
                    "Hidden blocks cannot be prose range endpoints: " + blockId.value());
        }
    }
}
