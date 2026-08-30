package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.StableIds;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.PortableArtifactPutRequest;
import dev.storyblock.storage.StoredArtifact;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlitePortableArtifactTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void portableImagePutIsAtomicIdempotentAndExportable() throws Exception {
        try (SqliteRevisionStore store = SqliteRevisionStore.open(
                temporaryDirectory.resolve("images.db")
        )) {
            var genesis = RevisionStoreTestFixture.genesis();
            String hash = RevisionStoreTestFixture.hash(genesis);
            store.createNovel(genesis, hash);
            var head = store.getHead(genesis.novel().id());
            String key = "upload-image-1";
            Ids.ArtifactId artifactId = new Ids.ArtifactId(StableIds.derive(
                    "art", genesis.novel().id().value(), "image-upload:" + key
            ));
            byte[] content = {1, 2, 3, 4};
            StoredArtifact artifact = new StoredArtifact(
                    artifactId,
                    genesis.novel().id(),
                    genesis.id(),
                    "narrative-image",
                    "image/png",
                    "identity",
                    CanonicalJson.hashBytes(content),
                    content,
                    Instant.parse("2026-08-29T00:00:00Z"),
                    true
            );

            var first = store.putPortableArtifact(
                    new PortableArtifactPutRequest(head, key, artifact)
            );
            var replay = store.putPortableArtifact(
                    new PortableArtifactPutRequest(head, key, artifact)
            );

            assertFalse(first.idempotentReplay());
            assertTrue(replay.idempotentReplay());
            assertArrayEquals(content, store.getArtifact(artifactId).content());
            assertEquals(1, store.loadCanonicalPackage(genesis.novel().id()).artifacts().size());

            byte[] changed = {4, 3, 2, 1};
            StoredArtifact conflicting = new StoredArtifact(
                    artifactId,
                    genesis.novel().id(),
                    genesis.id(),
                    "narrative-image",
                    "image/png",
                    "identity",
                    CanonicalJson.hashBytes(changed),
                    changed,
                    Instant.parse("2026-08-29T00:01:00Z"),
                    true
            );
            assertThrows(
                    IdempotencyConflictException.class,
                    () -> store.putPortableArtifact(
                            new PortableArtifactPutRequest(head, key, conflicting)
                    )
            );
        }
    }
}
