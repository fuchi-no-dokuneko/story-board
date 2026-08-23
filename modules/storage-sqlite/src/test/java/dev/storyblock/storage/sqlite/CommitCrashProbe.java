package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.nio.file.Path;
import java.time.Instant;

public final class CommitCrashProbe {
    private CommitCrashProbe() {
    }

    public static void main(String[] args) throws Exception {
        Path database = Path.of(args[0]);
        CommitStage target = CommitStage.valueOf(args[1]);
        try (SqliteRevisionStore store = SqliteRevisionStore.open(
                database,
                new CheckpointPolicy(1, 1),
                stage -> {
                    if (stage == target) {
                        Runtime.getRuntime().halt(23);
                    }
                }
        )) {
            var novelId = store.listNovels().getFirst();
            var head = store.getHead(novelId);
            var base = store.getRevision(novelId, head.revisionId()).manifest();
            var operation = RevisionStoreTestFixture.replace(
                    base, "crash-" + target, "第二句。"
            );
            store.commitCas(RevisionStoreTestFixture.request(
                    base,
                    operation,
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-23T09:00:00Z")
            ));
        }
        throw new IllegalStateException("Crash stage was not reached");
    }
}
