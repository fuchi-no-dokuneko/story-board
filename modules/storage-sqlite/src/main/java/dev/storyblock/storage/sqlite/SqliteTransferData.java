package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.storage.StoredExportJob;

final class SqliteTransferData {
    record LocatedBlock(Ids.SceneId sceneId, NarrativeBlock block) {
    }

    record ImportReceipt(String requestHash, Ids.NovelId novelId) {
    }

    record StoredExportWithHash(StoredExportJob job, String requestHash) {
    }
}
