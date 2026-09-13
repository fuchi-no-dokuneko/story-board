package dev.storyblock.api.http;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class RecentReadGuard {
    private final RecentReadLedger ledger;
    private final SqliteRevisionStore store;
    public RecentReadGuard(RecentReadLedger ledger, SqliteRevisionStore store) {
        this.ledger = ledger; this.store = store;
    }

    void require(EditOperation operation) {
        ledger.prune();
        var context = operation.context();
        if (store.findByIdempotencyKey(context.novelId(), context.idempotencyKey()).isPresent()) return;
        var revision = store.getRevision(context.novelId(), context.baseRevisionId()).manifest();
        var targets = ReadTargetSelection.select(revision, operation);
        var missing = new ArrayList<String>();
        var requests = new ArrayList<String>();
        String novel = context.novelId().value();
        String version = revision.id().value();
        for (var entry : targets.indices.entrySet()) {
            var scene = revision.requireScene(entry.getKey());
            var absent = new ArrayList<Integer>();
            for (int index : entry.getValue()) {
                String block = scene.blocks().get(index).id().value();
                if (!ledger.fresh(novel, version, block)) { absent.add(index); missing.add(block); }
            }
            boolean empty = scene.blocks().isEmpty() && !ledger.fresh(novel, version, scene.id().value());
            if (!absent.isEmpty() || empty) {
                int start = empty ? 0 : Collections.min(absent);
                int end = empty ? 0 : Collections.max(absent) + 1;
                requests.add("/v1/novels/" + novel + "/scenes/" + scene.id().value()
                        + "/blocks?revision_id=" + version + "&start=" + start + "&end=" + end);
            }
        }
        if (!requests.isEmpty()) throw new ApiFailureException(HttpStatus.CONFLICT, "RECENT_READ_REQUIRED",
                "Read the affected context first", "recent-read-required",
                "Call the read tool for the listed slices, then retry within 300 seconds. 請先讀取列出的區塊範圍，再於五分鐘內提交。",
                Map.of("missing_block_ids", missing, "read_requests", requests, "max_age_seconds", 300), null);
    }
}
