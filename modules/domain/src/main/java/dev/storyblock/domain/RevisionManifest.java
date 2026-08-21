package dev.storyblock.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RevisionManifest(
        Ids.RevisionId id,
        Ids.RevisionId parentId,
        Instant createdAt,
        NarrativeNovel novel
) {
    public RevisionManifest {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(novel, "novel");
        if (id.equals(parentId)) {
            throw new IllegalArgumentException("A revision cannot be its own parent");
        }
        validateGlobalIdentity(novel);
    }

    public Map<Ids.BlockId, Ids.BlockVersionId> selectedBlockVersions() {
        Map<Ids.BlockId, Ids.BlockVersionId> selected = new LinkedHashMap<>();
        for (NarrativeBlock block : liveBlocks()) {
            selected.put(block.id(), block.versionId());
        }
        return Collections.unmodifiableMap(selected);
    }

    public List<NarrativeBlock> liveBlocks() {
        List<NarrativeBlock> blocks = new ArrayList<>();
        for (NarrativeChapter chapter : novel.chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                blocks.addAll(scene.blocks());
            }
        }
        return List.copyOf(blocks);
    }

    public NarrativeScene requireScene(Ids.SceneId sceneId) {
        for (NarrativeChapter chapter : novel.chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                if (scene.id().equals(sceneId)) {
                    return scene;
                }
            }
        }
        throw new IllegalArgumentException("Revision does not contain scene " + sceneId.value());
    }

    public NarrativeBlock requireBlock(Ids.BlockId blockId) {
        for (NarrativeBlock block : liveBlocks()) {
            if (block.id().equals(blockId)) {
                return block;
            }
        }
        throw new IllegalArgumentException("Revision does not contain block " + blockId.value());
    }

    private static void validateGlobalIdentity(NarrativeNovel novel) {
        Map<Ids.SceneId, Boolean> scenes = new LinkedHashMap<>();
        Map<Ids.BlockId, Ids.BlockVersionId> selections = new LinkedHashMap<>();
        Map<Ids.BlockVersionId, Ids.BlockId> versions = new LinkedHashMap<>();
        for (NarrativeChapter chapter : novel.chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                if (scenes.put(scene.id(), Boolean.TRUE) != null) {
                    throw new IllegalArgumentException("Revision contains duplicate scene ID " + scene.id().value());
                }
                for (NarrativeBlock block : scene.blocks()) {
                    if (selections.put(block.id(), block.versionId()) != null) {
                        throw new IllegalArgumentException(
                                "A live block must have exactly one selected version: " + block.id().value()
                        );
                    }
                    if (versions.put(block.versionId(), block.id()) != null) {
                        throw new IllegalArgumentException(
                                "Block version cannot be selected by multiple live blocks: "
                                        + block.versionId().value()
                        );
                    }
                }
            }
        }
    }
}
