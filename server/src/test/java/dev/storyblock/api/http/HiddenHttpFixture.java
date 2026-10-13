package dev.storyblock.api.http;

import dev.storyblock.domain.*;
import java.util.*;

public final class HiddenHttpFixture {
    public static final String SENTINEL = "HIDDEN_SENTINEL_foreshadowing";

    public static RevisionManifest revision() {
        var base = NarrativeHttpFixture.revision(false);
        var chapter = base.novel().chapters().getFirst(); var scene = chapter.scenes().getFirst();
        var blocks = new ArrayList<>(scene.blocks());
        blocks.add(1,NarrativeBlock.create(Ids.BlockId.create(),
                OrderKey.between(blocks.get(0).orderKey(),blocks.get(1).orderKey()),
                SENTINEL+"\nLin entered. Mary left. Payoff in chapter 9. "+"象徵".repeat(80),
                BlockMetadata.empty(),Map.of(BlockHidden.EXTENSION_KEY,true)));
        return content(base,List.of(scene.withBlocks(blocks)));
    }

    public static RevisionManifest withoutHidden(RevisionManifest base) {
        return content(base,base.novel().chapters().getFirst().scenes().stream()
                .map(s -> s.withBlocks(NarrativeView.visibleBlocks(s))).toList());
    }

    private static RevisionManifest content(RevisionManifest base,List<NarrativeScene> scenes) {
        return new RevisionManifest(base.id(),base.parentId(),base.createdAt(),new NarrativeNovel(
                base.novel().id(),List.of(base.novel().chapters().getFirst().withScenes(scenes)),base.novel().extensions()));
    }
}
