package dev.storyblock.contracts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalNovelPackageTest {
    @Test
    void packageAndPortableArtifactRoundTripWithoutByteOrHashDrift() {
        CanonicalRevision revision = genesis();
        byte[] artifactContent = "portable trace".getBytes(StandardCharsets.UTF_8);
        CanonicalNovelPackage.ArtifactEntry artifact = new CanonicalNovelPackage.ArtifactEntry(
                Ids.ArtifactId.create(),
                NarrativeCanonicalMapper.fromCanonical(revision).id(),
                "detector-trace",
                "application/json",
                "identity",
                CanonicalJson.hashBytes(artifactContent),
                artifactContent,
                Instant.parse("2026-08-21T12:01:00Z")
        );
        CanonicalNovelPackage first = CanonicalNovelPackage.assemble(
                List.of(new CanonicalNovelPackage.RevisionEntry(0, revision)),
                List.of(),
                List.of(artifact)
        );

        CanonicalNovelPackage parsed = CanonicalNovelPackage.parse(first.bytes());

        assertArrayEquals(first.bytes(), parsed.bytes());
        assertEquals(first.packageHash(), parsed.packageHash());
        assertEquals(revision.contentHash(), parsed.manifest().headHash());
        assertEquals(1, parsed.manifest().revisionCount());
        assertEquals(1, parsed.manifest().artifactCount());
        assertArrayEquals(artifactContent, parsed.artifacts().getFirst().content());
    }

    @Test
    void unsupportedVersionsUnknownFieldsAndDuplicateKeysAreRejected() {
        String canonical = new String(
                CanonicalNovelPackage.genesis(genesis()).bytes(),
                StandardCharsets.UTF_8
        );
        byte[] unsupported = canonical.replace(
                "\"package_version\":\"1.0.0\"",
                "\"package_version\":\"2.0.0\""
        ).getBytes(StandardCharsets.UTF_8);
        byte[] unknown = canonical.replaceFirst(
                "\\{",
                "{\"unsafe\":true,"
        ).getBytes(StandardCharsets.UTF_8);
        byte[] duplicate = canonical.replaceFirst(
                "\\{",
                "{\"package_version\":\"1.0.0\","
        ).getBytes(StandardCharsets.UTF_8);

        assertThrows(CanonicalPackageException.class, () -> CanonicalNovelPackage.parse(unsupported));
        assertThrows(CanonicalPackageException.class, () -> CanonicalNovelPackage.parse(unknown));
        assertThrows(CanonicalPackageException.class, () -> CanonicalNovelPackage.parse(duplicate));
    }

    @Test
    void manifestAndArtifactTamperingAreRejected() {
        CanonicalRevision revision = genesis();
        byte[] content = "trace".getBytes(StandardCharsets.UTF_8);
        CanonicalNovelPackage value = CanonicalNovelPackage.assemble(
                List.of(new CanonicalNovelPackage.RevisionEntry(0, revision)),
                List.of(),
                List.of(new CanonicalNovelPackage.ArtifactEntry(
                        Ids.ArtifactId.create(),
                        NarrativeCanonicalMapper.fromCanonical(revision).id(),
                        "trace-data",
                        "application/json",
                        "identity",
                        CanonicalJson.hashBytes(content),
                        content,
                        Instant.parse("2026-08-21T12:01:00Z")
                ))
        );
        String canonical = new String(value.bytes(), StandardCharsets.UTF_8);
        byte[] wrongCount = canonical.replace(
                "\"artifact_count\":1",
                "\"artifact_count\":2"
        ).getBytes(StandardCharsets.UTF_8);
        byte[] wrongSize = canonical.replace(
                "\"size_bytes\":5",
                "\"size_bytes\":6"
        ).getBytes(StandardCharsets.UTF_8);
        byte[] wrongContent = canonical.replace(
                "\"content_base64\":\"dHJhY2U=\"",
                "\"content_base64\":\"dHJhY2Y=\""
        ).getBytes(StandardCharsets.UTF_8);

        assertThrows(CanonicalPackageException.class, () -> CanonicalNovelPackage.parse(wrongCount));
        assertThrows(CanonicalPackageException.class, () -> CanonicalNovelPackage.parse(wrongSize));
        assertThrows(CanonicalPackageException.class, () -> CanonicalNovelPackage.parse(wrongContent));
    }

    @Test
    void standaloneRevisionMustBeGenesis() {
        CanonicalRevision genesis = genesis();
        RevisionManifest original = NarrativeCanonicalMapper.fromCanonical(genesis);
        RevisionManifest child = new RevisionManifest(
                Ids.RevisionId.create(),
                original.id(),
                Instant.parse("2026-08-21T12:01:00Z"),
                original.novel()
        );

        assertThrows(
                CanonicalPackageException.class,
                () -> CanonicalNovelPackage.genesis(
                        NarrativeCanonicalMapper.toCanonical(child)
                )
        );
    }

    private static CanonicalRevision genesis() {
        Ids.NovelId novelId = Ids.NovelId.create();
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
                "Opening",
                TransitionMode.OPENING,
                SceneSeed.empty(),
                List.of(block),
                Map.of()
        );
        RevisionManifest manifest = new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                Instant.parse("2026-08-21T12:00:00Z"),
                new NarrativeNovel(
                        novelId,
                        List.of(new NarrativeChapter(
                                chapterId,
                                OrderKey.initial(),
                                "Chapter",
                                List.of(scene),
                                Map.of()
                        )),
                        Map.of()
                )
        );
        return NarrativeCanonicalMapper.toCanonical(manifest);
    }
}
