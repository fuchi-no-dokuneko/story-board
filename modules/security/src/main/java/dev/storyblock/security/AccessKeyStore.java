package dev.storyblock.security;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccessKeyStore {
    AccessKeyInsertResult issueAccessKey(
            StoredAccessKey key,
            String idempotencyKey,
            String requestHash,
            AuditContext auditContext
    );

    Optional<StoredAccessKey> findAccessKey(Ids.AccessKeyId keyId);

    boolean revokeAccessKey(
            Ids.AccessKeyId keyId,
            Ids.NovelId expectedNovelId,
            AuditContext auditContext
    );

    boolean touchAccessKeyLastUsed(
            Ids.AccessKeyId keyId,
            Instant usedAt,
            Instant staleBefore
    );

    void appendAuditEvent(AuditEvent event);

    List<AuditEvent> listAuditEvents(Ids.NovelId novelId);
}
