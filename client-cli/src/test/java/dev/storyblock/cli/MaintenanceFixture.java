package dev.storyblock.cli;

import dev.storyblock.domain.*;
import java.time.Instant;
import java.util.*;

final class MaintenanceFixture {
    static RevisionManifest book(String title, Map<String,Object> image) {
        var chapter = Ids.ChapterId.create();
        var block = NarrativeBlock.create(Ids.BlockId.create(),OrderKey.initial(),"雨水落在窗邊。",BlockMetadata.empty(),image);
        var scene = new NarrativeScene(Ids.SceneId.create(),chapter,OrderKey.initial(),"窗邊",TransitionMode.OPENING,
                SceneSeed.empty(),List.of(block),Map.of());
        return new RevisionManifest(Ids.RevisionId.create(),null,Instant.parse("2026-09-21T00:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(),List.of(new NarrativeChapter(chapter,OrderKey.initial(),title,List.of(scene),Map.of())),Map.of("title",title)));
    }
}
