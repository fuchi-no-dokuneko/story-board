package dev.storyblock.application;

import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.*;
import java.time.Instant;
import java.util.*;

final class HiddenBlockFixture {
    static final String SENTINEL = "HIDDEN_SENTINEL_foreshadowing";
    static final String HASH = "sha256:" + "a".repeat(64);

    static RevisionManifest revision() {
        var chapterId = Ids.ChapterId.create();
        var scenes = new ArrayList<NarrativeScene>();
        for (int s = 0; s < 2; s++) {
            var blocks = new ArrayList<NarrativeBlock>();
            for (int b = 0; b < 5; b++) {
                boolean hidden = b % 2 == 0;
                blocks.add(NarrativeBlock.create(Ids.BlockId.create(), OrderKey.rebalanced(b,5),
                        hidden ? SENTINEL + "\nLin entered. Mary left. Payoff in chapter 9. " + "象徵".repeat(80)
                                : b == 1 ? "雨水落在窗邊。" : "她把茶杯放下。",
                        BlockMetadata.empty(), hidden ? Map.of(BlockHidden.EXTENSION_KEY,true) : Map.of()));
            }
            scenes.add(new NarrativeScene(Ids.SceneId.create(),chapterId,OrderKey.rebalanced(s,2),
                    "Scene " + s,s == 0 ? TransitionMode.OPENING : TransitionMode.CONTINUOUS,
                    new SceneSeed(Map.of("present_character_ids",List.of("Lin"))),blocks,Map.of()));
        }
        return new RevisionManifest(Ids.RevisionId.create(),null,Instant.parse("2026-10-13T12:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(),List.of(new NarrativeChapter(chapterId,
                        OrderKey.initial(),"Chapter",scenes,Map.of())),Map.of("title","Teacup")));
    }

    static RevisionManifest withoutHidden(RevisionManifest revision) {
        var novel = revision.novel();
        var chapters = novel.chapters().stream().map(c -> c.withScenes(c.scenes().stream()
                .map(s -> s.withBlocks(NarrativeView.visibleBlocks(s))).toList())).toList();
        return new RevisionManifest(revision.id(),revision.parentId(),revision.createdAt(),
                new NarrativeNovel(novel.id(),chapters,novel.extensions()));
    }

    static EditContext context(RevisionManifest revision) {
        return new EditContext(Ids.OperationId.create(),UUID.randomUUID().toString(),
                revision.novel().id(),revision.id(),NarrativeCanonicalMapper.toCanonical(revision).contentHash());
    }
}
