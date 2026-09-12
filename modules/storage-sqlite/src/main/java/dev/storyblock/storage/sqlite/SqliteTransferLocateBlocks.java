package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import java.util.LinkedHashMap;
import java.util.Map;
import static dev.storyblock.storage.sqlite.SqliteTransferData.*;

final class SqliteTransferLocateBlocks {
    static Map<Ids.BlockId, LocatedBlock> locateBlocks(RevisionManifest revision) {
        Map<Ids.BlockId, LocatedBlock> blocks = new LinkedHashMap<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                for (NarrativeBlock block : scene.blocks()) {
                    blocks.put(block.id(), new LocatedBlock(scene.id(), block));
                }
            }
        }
        return blocks;
    }
}
