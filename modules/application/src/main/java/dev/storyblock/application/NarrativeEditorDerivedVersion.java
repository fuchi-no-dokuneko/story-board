package dev.storyblock.application;

import dev.storyblock.domain.Ids;

final class NarrativeEditorDerivedVersion {
    static Ids.BlockVersionId derivedVersion(
            Ids.OperationId operationId,
            Ids.BlockId blockId
    ) {
        return new Ids.BlockVersionId(dev.storyblock.domain.StableIds.derive(
                "blv", operationId.value(), "block-version:" + blockId.value()
        ));
    }
}
