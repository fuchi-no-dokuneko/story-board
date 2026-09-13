package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.*;
import dev.storyblock.storage.IdentityConflictException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ShortIdOwnershipTest {
    @TempDir Path directory;
    @Test void persistedIdsCannotBeReusedByAnotherNovel() throws Exception {
        try (var store = SqliteRevisionStore.open(directory.resolve("owners.db"))) {
            var first = RevisionStoreTestFixture.genesis();
            store.createNovel(first, NarrativeCanonicalMapper.toCanonical(first).contentHash());
            var conflicting = new RevisionManifest(Ids.RevisionId.create(), null, first.createdAt(),
                    new NarrativeNovel(Ids.NovelId.create(), first.novel().chapters(), Map.of()));
            assertThrows(IdentityConflictException.class, () -> store.createNovel(conflicting,
                    NarrativeCanonicalMapper.toCanonical(conflicting).contentHash()));
            assertEquals(1, store.revisionCount(first.novel().id()));
        }
    }

    @Test void durableAllocationsCanBeClaimedAndRetriedAfterReopening() throws Exception {
        Path path = directory.resolve("scoped.db");
        RevisionManifest revision;
        try (var store = SqliteRevisionStore.open(path);
             var scope = ShortIdScope.open(new SqliteShortIds(store))) {
            revision = RevisionStoreTestFixture.genesis();
            store.createNovel(revision, NarrativeCanonicalMapper.toCanonical(revision).contentHash());
        }
        try (var store = SqliteRevisionStore.open(path)) {
            var request = RevisionStoreTestFixture.replace(revision, "replace", "第二句。");
            try (var scope = ShortIdScope.open(new SqliteShortIds(store))) {
                var commit = RevisionStoreTestFixture.request(revision, request, Ids.RevisionId.create(), revision.createdAt().plusSeconds(1));
                var result = store.commitCas(commit);
                assertEquals(revision.liveBlocks().getFirst().id(), commit.candidate().liveBlocks().getFirst().id());
                assertNotEquals(revision.liveBlocks().getFirst().versionId(), commit.candidate().liveBlocks().getFirst().versionId());
                assertEquals(result.revision(), store.commitCas(commit).revision());
            }
        }
    }
}
