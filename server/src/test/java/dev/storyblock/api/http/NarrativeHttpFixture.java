package dev.storyblock.api.http;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.*;
import java.time.Instant;
import java.util.*;

final class NarrativeHttpFixture {
    static RevisionManifest revision(boolean empty) {
        var chapterId = Ids.ChapterId.create();
        var blocks = new ArrayList<NarrativeBlock>();
        if (!empty) for (int i = 0; i < 3; i++) blocks.add(NarrativeBlock.create(Ids.BlockId.create(),
                OrderKey.rebalanced(i, 3), List.of("晨霧散了。", "她看見山谷🙂。", "遠方傳來回聲。").get(i), BlockMetadata.empty(), Map.of()));
        var scene = new NarrativeScene(Ids.SceneId.create(), chapterId, OrderKey.initial(), "山谷",
                TransitionMode.OPENING, SceneSeed.empty(), blocks, Map.of());
        var chapter = new NarrativeChapter(chapterId, OrderKey.initial(), "第一章", List.of(scene), Map.of());
        return new RevisionManifest(Ids.RevisionId.create(), null, Instant.parse("2026-09-12T12:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(), List.of(chapter), Map.of()));
    }
    static Map<String, Object> insertion(RevisionManifest revision) {
        var scene = revision.novel().chapters().getFirst().scenes().getFirst();
        var point = new LinkedHashMap<String, Object>();
        point.put("scene_id", scene.id().value());
        point.put("position", scene.blocks().isEmpty() ? "start" : "after");
        if (!scene.blocks().isEmpty()) point.put("anchor_block_id", scene.blocks().getFirst().id().value());
        return Map.of("operation", Map.of("operation_id", Ids.OperationId.create().value(),
                "idempotency_key", "insert-" + revision.id().value(), "novel_id", revision.novel().id().value(),
                "base_revision_id", revision.id().value(), "expected_head_hash", hash(revision),
                "type", "insert_blocks", "payload", Map.of("insertion_point", point,
                    "blocks", List.of(Map.of("id", Ids.BlockId.create().value(), "text", "她繼續向前走。", "meta", Map.of())))),
                "candidate_revision_id", Ids.RevisionId.create().value(), "candidate_created_at", "2026-09-12T12:01:00Z");
    }
    static String hash(RevisionManifest revision) { return NarrativeCanonicalMapper.toCanonical(revision).contentHash(); }
}
