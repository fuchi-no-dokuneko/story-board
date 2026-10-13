package dev.storyblock.api.http;

import dev.storyblock.domain.*;
import java.time.Instant;
import java.util.*;

final class HiddenRewriteFixture {
    static RevisionManifest revision() {
        var chapter = Ids.ChapterId.create(); var blocks = new ArrayList<NarrativeBlock>();
        for (int i=0;i<202;i++) {
            boolean hidden = i==0||i==70;
            blocks.add(NarrativeBlock.create(Ids.BlockId.create(),OrderKey.rebalanced(i,202),
                    hidden?HiddenHttpFixture.SENTINEL+"\nPayoff in chapter 9.":
                            "他在雨中聽見"+"風穿過狹窄街道".repeat(10)+"。",
                    BlockMetadata.empty(),hidden?Map.of(BlockHidden.EXTENSION_KEY,true):Map.of()));
        }
        var scene = new NarrativeScene(Ids.SceneId.create(),chapter,OrderKey.initial(),"Rain",
                TransitionMode.OPENING,SceneSeed.empty(),blocks,Map.of());
        return new RevisionManifest(Ids.RevisionId.create(),null,Instant.parse("2026-10-13T12:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(),List.of(new NarrativeChapter(chapter,OrderKey.initial(),
                        "Rain",List.of(scene),Map.of())),Map.of()));
    }
}
