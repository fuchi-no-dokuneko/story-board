package dev.storyblock.application;

import static dev.storyblock.application.RevisionDiff.LocatedBlock;

final class RevisionDiffChange {
    static BlockChange change(
            BlockChange.Type type,
            LocatedBlock oldBlock,
            LocatedBlock newBlock
    ) {
        LocatedBlock identity = oldBlock == null ? newBlock : oldBlock;
        return new BlockChange(
                type,
                identity.block().id(),
                oldBlock == null ? null : oldBlock.block().versionId(),
                newBlock == null ? null : newBlock.block().versionId(),
                oldBlock == null ? null : oldBlock.sceneId(),
                newBlock == null ? null : newBlock.sceneId(),
                oldBlock == null ? null : oldBlock.index(),
                newBlock == null ? null : newBlock.index(),
                oldBlock == null ? null : oldBlock.block().text(),
                newBlock == null ? null : newBlock.block().text()
        );
    }
}
