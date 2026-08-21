package dev.storyblock.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.BlockVersionRef;
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
import dev.storyblock.validator.ValidationCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PreviewServiceTest {
    private static final Instant CANDIDATE_TIME = Instant.parse("2026-08-21T12:01:00Z");

    @Test
    void previewReturnsEveryFieldNormalizesExtendAndDoesNotMutateHeadOrBase() {
        RevisionManifest base = revision();
        byte[] baseBytes = NarrativeCanonicalMapper.toCanonical(base).canonicalBytes();
        Map<Ids.BlockId, Ids.BlockVersionId> baseSelections = base.selectedBlockVersions();
        AtomicReference<Ids.RevisionId> head = new AtomicReference<>(base.id());
        PreviewService service = new PreviewService(id -> {
            throw new IllegalArgumentException("Restore is not used in this preview");
        });
        NarrativeBlock original = base.liveBlocks().getFirst();
        EditOperation operation = new EditOperation.ExtendBlock(
                context(base, "preview-extend"),
                BlockRangeGuard.capture(firstScene(base), original.id(), original.id()),
                EditOperation.ExtensionPosition.AFTER,
                new BlockDraft(
                        original.id(),
                        original.text() + "補充一句。",
                        original.metadata(),
                        original.extensions()
                )
        );
        Ids.RevisionId candidateId = Ids.RevisionId.create();

        PreviewResponse first = service.preview(base, operation, candidateId, CANDIDATE_TIME);
        PreviewResponse repeated = service.preview(base, operation, candidateId, CANDIDATE_TIME);

        assertEquals(base.id(), first.baseRevisionId());
        assertEquals(NarrativeCanonicalMapper.toCanonical(base).contentHash(), first.baseHash());
        assertEquals("replace_block_range", first.normalizedOperation().get("type"));
        assertNotNull(first.candidateHash());
        assertEquals(64 + "sha256:".length(), first.candidateHash().length());
        assertEquals(1, first.diff().blockChanges().size());
        assertEquals(BlockChange.Type.MODIFIED, first.diff().blockChanges().getFirst().type());
        assertNotNull(first.renderPacket());
        assertEquals(candidateId, first.renderPacket().revisionId());
        assertTrue(first.violations().isEmpty());
        assertTrue(first.warnings().isEmpty());
        assertTrue(first.committable());
        assertEquals(Set.of(
                "base_revision_id",
                "base_hash",
                "normalized_operation",
                "candidate_hash",
                "diff",
                "render_packet",
                "violations",
                "warnings",
                "committable"
        ), first.contractFields().keySet());
        assertEquals(first.candidateHash(), repeated.candidateHash());
        assertArrayEquals(
                CanonicalJson.bytes(first.renderPacket()),
                CanonicalJson.bytes(repeated.renderPacket())
        );

        assertEquals(base.id(), head.get());
        assertEquals(baseSelections, base.selectedBlockVersions());
        assertArrayEquals(baseBytes, NarrativeCanonicalMapper.toCanonical(base).canonicalBytes());
    }

    @Test
    void invalidDraftTextReturnsStructuredNonCanonicalPreviewWithoutCreatingARevision() {
        RevisionManifest base = revision();
        NarrativeBlock original = base.liveBlocks().getFirst();
        EditOperation operation = new EditOperation.ReplaceBlockRange(
                context(base, "invalid-text"),
                BlockRangeGuard.capture(firstScene(base), original.id(), original.id()),
                List.of(new BlockDraft(
                        original.id(), "未完成", original.metadata(), original.extensions()
                ))
        );

        PreviewResponse preview = service().preview(
                base, operation, Ids.RevisionId.create(), CANDIDATE_TIME
        );

        assertFalse(preview.committable());
        assertTrue(preview.violations().stream()
                .anyMatch(issue -> issue.code() == ValidationCode.INVALID_SENTENCE_COUNT));
        assertNotNull(preview.candidateHash());
        assertTrue(preview.diff().blockChanges().isEmpty());
        assertNull(preview.renderPacket());
    }

    @Test
    void canonicalCandidateWithMetadataErrorStillReturnsDiffAndRenderPacket() {
        RevisionManifest base = revision();
        NarrativeBlock original = base.liveBlocks().getFirst();
        EditOperation operation = new EditOperation.CorrectBlockMeta(
                context(base, "absent-speaker"),
                firstScene(base).id(),
                BlockVersionRef.from(original),
                new BlockMetadata(Map.of(
                        "speech", Map.of("direct_speaker_id", "char_absent")
                ))
        );

        PreviewResponse preview = service().preview(
                base, operation, Ids.RevisionId.create(), CANDIDATE_TIME
        );

        assertFalse(preview.committable());
        assertTrue(preview.violations().stream()
                .anyMatch(issue -> issue.code() == ValidationCode.SPEAKER_NOT_PRESENT));
        assertNotNull(preview.renderPacket());
        assertEquals(1, preview.diff().blockChanges().stream()
                .filter(change -> change.type() == BlockChange.Type.MODIFIED)
                .count());
    }

    @Test
    void staleHeadReturnsRevisionConflictWithoutApplyingTheOperation() {
        RevisionManifest base = revision();
        NarrativeBlock original = base.liveBlocks().getFirst();
        EditContext stale = new EditContext(
                Ids.OperationId.create(),
                "stale-preview",
                base.novel().id(),
                base.id(),
                "sha256:" + "0".repeat(64)
        );
        EditOperation operation = new EditOperation.DeleteBlockRange(
                stale,
                BlockRangeGuard.capture(firstScene(base), original.id(), original.id())
        );

        PreviewResponse preview = service().preview(
                base, operation, Ids.RevisionId.create(), CANDIDATE_TIME
        );

        assertFalse(preview.committable());
        assertEquals(ValidationCode.REVISION_CONFLICT, preview.violations().getFirst().code());
        assertNull(preview.renderPacket());
        assertEquals(1, base.liveBlocks().size());
    }

    private static PreviewService service() {
        return new PreviewService(id -> {
            throw new IllegalArgumentException("No restore revision " + id.value());
        });
    }

    private static EditContext context(RevisionManifest base, String key) {
        return new EditContext(
                Ids.OperationId.create(),
                key,
                base.novel().id(),
                base.id(),
                NarrativeCanonicalMapper.toCanonical(base).contentHash()
        );
    }

    private static RevisionManifest revision() {
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
                Instant.parse("2026-08-21T12:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(), List.of(chapter), Map.of())
        );
    }

    private static NarrativeScene firstScene(RevisionManifest revision) {
        return revision.novel().chapters().getFirst().scenes().getFirst();
    }
}
