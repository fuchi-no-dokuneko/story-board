package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredCheckpoint;
import dev.storyblock.storage.StoredOperation;
import dev.storyblock.storage.StoredRevision;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReplayService {
    private final RevisionStore store;

    public ReplayService(RevisionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public ReplayResult materialize(
            Ids.NovelId novelId,
            Ids.RevisionId targetRevisionId
    ) {
        StoredRevision target = store.getRevision(novelId, targetRevisionId);
        StoredCheckpoint checkpoint = store.loadCheckpoint(novelId, target.sequence())
                .orElseThrow(() -> failure(
                        novelId, target.sequence(), "No checkpoint exists at or before target"
                ));
        RevisionManifest start = readCheckpoint(novelId, checkpoint);
        Map<Ids.RevisionId, RevisionManifest> restored = new HashMap<>();
        restored.put(start.id(), start);
        return replayRange(
                novelId,
                start,
                checkpoint.sequence(),
                target,
                revisionId -> restored.computeIfAbsent(
                        revisionId,
                        id -> materializeFull(novelId, id).revision()
                ),
                restored
        );
    }

    public ReplayResult materializeFull(
            Ids.NovelId novelId,
            Ids.RevisionId targetRevisionId
    ) {
        StoredRevision target = store.getRevision(novelId, targetRevisionId);
        StoredRevision genesis = store.getRevisionAtSequence(novelId, 0);
        if (genesis.manifest().parentId() != null) {
            throw failure(novelId, 0, "Genesis revision has a parent");
        }
        Map<Ids.RevisionId, RevisionManifest> replayed = new HashMap<>();
        replayed.put(genesis.manifest().id(), genesis.manifest());
        RevisionLookup lookup = revisionId -> {
            RevisionManifest revision = replayed.get(revisionId);
            if (revision == null) {
                throw failure(
                        novelId,
                        -1,
                        "Restore target was not present in replayed history: "
                                + revisionId.value()
                );
            }
            return revision;
        };
        return replayRange(novelId, genesis.manifest(), 0, target, lookup, replayed);
    }

    public ReplayVerificationReport verifyAllHeads() {
        List<ReplayVerification> results = store.listNovels().stream()
                .map(this::verifyHead)
                .toList();
        return new ReplayVerificationReport(results);
    }

    private ReplayVerification verifyHead(Ids.NovelId novelId) {
        RevisionRef head = null;
        try {
            head = store.getHead(novelId);
            ReplayResult result = materializeFull(novelId, head.revisionId());
            boolean valid = head.contentHash().equals(result.contentHash())
                    && head.sequence() == result.targetSequence();
            return new ReplayVerification(
                    novelId,
                    head.revisionId(),
                    head.contentHash(),
                    result.contentHash(),
                    result.replayedOperations(),
                    valid,
                    valid ? "head hash reproduced" : "head reference did not match replay"
            );
        } catch (RuntimeException exception) {
            return new ReplayVerification(
                    novelId,
                    head == null ? null : head.revisionId(),
                    head == null ? null : head.contentHash(),
                    null,
                    0,
                    false,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
        }
    }

    private ReplayResult replayRange(
            Ids.NovelId novelId,
            RevisionManifest start,
            long startingSequence,
            StoredRevision target,
            RevisionLookup revisionLookup,
            Map<Ids.RevisionId, RevisionManifest> replayed
    ) {
        if (startingSequence > target.sequence()) {
            throw failure(novelId, target.sequence(), "Replay starts after target revision");
        }
        List<StoredOperation> operations = store.listOperations(
                novelId, startingSequence, target.sequence()
        );
        long requiredCount = target.sequence() - startingSequence;
        if (operations.size() != requiredCount) {
            throw failure(
                    novelId,
                    target.sequence(),
                    "Operation log has a gap: expected " + requiredCount
                            + " entries but found " + operations.size()
            );
        }

        RevisionManifest current = start;
        String currentHash = NarrativeCanonicalMapper.toCanonical(current).contentHash();
        NarrativeEditor editor = new NarrativeEditor(revisionLookup);
        long expectedSequence = startingSequence + 1;
        for (StoredOperation stored : operations) {
            if (stored.sequence() != expectedSequence) {
                throw failure(novelId, expectedSequence, "Operation sequence is not contiguous");
            }
            EditOperation operation = stored.operation();
            if (!operation.context().novelId().equals(novelId)
                    || !operation.context().baseRevisionId().equals(current.id())
                    || !operation.context().expectedHeadHash().equals(currentHash)) {
                throw failure(
                        novelId,
                        expectedSequence,
                        "Operation base identity or hash does not match replay state"
                );
            }
            try {
                current = editor.apply(
                        current,
                        operation,
                        stored.resultRevisionId(),
                        stored.committedAt()
                );
            } catch (RuntimeException exception) {
                throw new ReplayException(
                        novelId,
                        expectedSequence,
                        "Operation could not be replayed",
                        exception
                );
            }
            currentHash = NarrativeCanonicalMapper.toCanonical(current).contentHash();
            if (!currentHash.equals(stored.resultHash())) {
                throw failure(
                        novelId,
                        expectedSequence,
                        "Operation result hash does not match replayed content"
                );
            }
            StoredRevision relational = store.getRevisionAtSequence(novelId, expectedSequence);
            if (!relational.manifest().equals(current)
                    || !relational.contentHash().equals(currentHash)) {
                throw failure(
                        novelId,
                        expectedSequence,
                        "Stored revision does not match operation replay"
                );
            }
            replayed.put(current.id(), current);
            expectedSequence++;
        }

        if (!current.id().equals(target.manifest().id())
                || !currentHash.equals(target.contentHash())) {
            throw failure(
                    novelId, target.sequence(), "Replay did not reproduce the target revision"
            );
        }
        return new ReplayResult(
                current,
                currentHash,
                target.sequence(),
                startingSequence,
                requiredCount
        );
    }

    private RevisionManifest readCheckpoint(
            Ids.NovelId novelId,
            StoredCheckpoint checkpoint
    ) {
        CanonicalRevision canonical;
        try {
            canonical = CanonicalRevision.parseEnvelope(store.decompressCheckpoint(checkpoint));
        } catch (RuntimeException exception) {
            throw new ReplayException(
                    novelId,
                    checkpoint.sequence(),
                    "Checkpoint payload is invalid",
                    exception
            );
        }
        RevisionManifest revision = NarrativeCanonicalMapper.fromCanonical(canonical);
        StoredRevision relational = store.getRevisionAtSequence(novelId, checkpoint.sequence());
        if (!canonical.contentHash().equals(checkpoint.contentHash())
                || !revision.novel().id().equals(novelId)
                || !revision.id().equals(checkpoint.revisionId())
                || !relational.manifest().equals(revision)
                || !relational.contentHash().equals(checkpoint.contentHash())) {
            throw failure(
                    novelId,
                    checkpoint.sequence(),
                    "Checkpoint identity or hash does not match canonical revision"
            );
        }
        return revision;
    }

    private static ReplayException failure(
            Ids.NovelId novelId,
            long sequence,
            String message
    ) {
        return new ReplayException(novelId, sequence, message);
    }
}
