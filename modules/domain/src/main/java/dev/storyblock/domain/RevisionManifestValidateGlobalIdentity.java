package dev.storyblock.domain;

import java.util.LinkedHashMap;
import java.util.Map;

final class RevisionManifestValidateGlobalIdentity {
    static void validateGlobalIdentity(NarrativeNovel novel) {
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
