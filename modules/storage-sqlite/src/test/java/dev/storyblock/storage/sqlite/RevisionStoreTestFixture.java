package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.application.NarrativeEditor;
import dev.storyblock.storage.CommitRequest;
import dev.storyblock.storage.RevisionRef;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class RevisionStoreTestFixture {
    static final Instant GENESIS_TIME = Instant.parse("2026-08-21T12:00:00Z");

    private RevisionStoreTestFixture() {
    }

    static RevisionManifest genesis() {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        NarrativeBlock block = NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.initial(),
                "第一句。",
                BlockMetadata.empty(),
                Map.of()
        );
        NarrativeScene scene = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.initial(),
                "Scene",
                TransitionMode.OPENING,
                SceneSeed.empty(),
                List.of(block),
                Map.of()
        );
        NarrativeChapter chapter = new NarrativeChapter(
                chapterId, OrderKey.initial(), "Chapter", List.of(scene), Map.of()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                GENESIS_TIME,
                new NarrativeNovel(Ids.NovelId.create(), List.of(chapter), Map.of())
        );
    }

    static EditOperation replace(RevisionManifest base, String key, String text) {
        NarrativeBlock current = base.liveBlocks().getFirst();
        return new EditOperation.ReplaceBlockRange(
                context(base, key),
                BlockRangeGuard.capture(scene(base), current.id(), current.id()),
                List.of(new BlockDraft(
                        current.id(), text, current.metadata(), current.extensions()
                ))
        );
    }

    static EditOperation delete(RevisionManifest base, String key) {
        NarrativeBlock current = base.liveBlocks().getFirst();
        return new EditOperation.DeleteBlockRange(
                context(base, key),
                BlockRangeGuard.capture(scene(base), current.id(), current.id())
        );
    }

    static EditOperation restore(
            RevisionManifest base,
            RevisionManifest target,
            String key
    ) {
        return new EditOperation.RestoreRevisionContent(
                context(base, key),
                target.id(),
                NarrativeCanonicalMapper.toCanonical(target).contentHash()
        );
    }

    static CommitRequest request(
            RevisionManifest base,
            EditOperation operation,
            Ids.RevisionId candidateId,
            Instant committedAt
    ) {
        RevisionManifest candidate = new NarrativeEditor(revisionId -> {
            if (base.id().equals(revisionId)) {
                return base;
            }
            throw new IllegalArgumentException("Unknown test revision " + revisionId.value());
        }).apply(base, operation, candidateId, committedAt);
        String baseHash = NarrativeCanonicalMapper.toCanonical(base).contentHash();
        String candidateHash = NarrativeCanonicalMapper.toCanonical(candidate).contentHash();
        return new CommitRequest(
                new RevisionRef(base.id(), 0, baseHash),
                operation,
                EditOperationCanonicalMapper.hash(operation),
                candidate,
                candidateHash
        );
    }

    static String hash(RevisionManifest revision) {
        return NarrativeCanonicalMapper.toCanonical(revision).contentHash();
    }

    private static EditContext context(RevisionManifest base, String key) {
        return new EditContext(
                Ids.OperationId.create(),
                key,
                base.novel().id(),
                base.id(),
                hash(base)
        );
    }

    private static NarrativeScene scene(RevisionManifest revision) {
        return revision.novel().chapters().getFirst().scenes().getFirst();
    }
}
