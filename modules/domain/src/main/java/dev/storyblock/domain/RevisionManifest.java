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
        RevisionManifestValidateGlobalIdentity.validateGlobalIdentity(novel);
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

}
