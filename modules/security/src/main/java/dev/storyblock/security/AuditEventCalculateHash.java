package dev.storyblock.security;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

final class AuditEventCalculateHash {
    static String calculateHash(
            Ids.AuditEventId eventId,
            Instant occurredAt,
            String requestId,
            String actorId,
            Ids.AccessKeyId actorKeyId,
            Ids.NovelId novelId,
            AuditAction action,
            String subjectId,
            Ids.OperationId operationId,
            Ids.RevisionId revisionId,
            AuditResult result,
            String operationHash,
            String contentHash
    ) {
        Map<String, Object> hashInput = new TreeMap<>();
        hashInput.put("event_id", eventId.value());
        hashInput.put("occurred_at", occurredAt.toString());
        hashInput.put("request_id", requestId);
        hashInput.put("actor_id", actorId);
        hashInput.put("actor_key_id", actorKeyId == null ? null : actorKeyId.value());
        hashInput.put("novel_id", novelId.value());
        hashInput.put("action", action.canonicalName());
        hashInput.put("subject_id", subjectId);
        hashInput.put("operation_id", operationId == null ? null : operationId.value());
        hashInput.put("revision_id", revisionId == null ? null : revisionId.value());
        hashInput.put("result", result.canonicalName());
        hashInput.put("operation_hash", operationHash);
        hashInput.put("content_hash", contentHash);
        return CanonicalJson.hash(hashInput);
    }
}
