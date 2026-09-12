package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import dev.storyblock.security.*;
import dev.storyblock.monitor.*;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

interface SqliteAccessKeyPort extends AccessKeyStore, SqliteStoreContext {
  @Override
  default AccessKeyInsertResult issueAccessKey(
      StoredAccessKey key,
      String idempotencyKey,
      String requestHash,
      AuditContext auditContext
  ) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(auditContext, "auditContext");
    return context().write(connection -> SqliteSecurityIssueAccessKey.issueAccessKey(
        connection, key, idempotencyKey, requestHash, auditContext
    ));
  }

  @Override
  default Optional<StoredAccessKey> findAccessKey(Ids.AccessKeyId keyId) {
    Objects.requireNonNull(keyId, "keyId");
    return context().read(connection -> SqliteSecurityFindAccessKey.findAccessKey(connection, keyId));
  }

  @Override
  default boolean revokeAccessKey(
      Ids.AccessKeyId keyId,
      Ids.NovelId expectedNovelId,
      AuditContext auditContext
  ) {
    Objects.requireNonNull(keyId, "keyId");
    Objects.requireNonNull(expectedNovelId, "expectedNovelId");
    Objects.requireNonNull(auditContext, "auditContext");
    return context().write(connection -> SqliteSecurityRevokeAccessKey.revokeAccessKey(
        connection, keyId, expectedNovelId, auditContext
    ));
  }

  @Override
  default boolean touchAccessKeyLastUsed(
      Ids.AccessKeyId keyId,
      Instant usedAt,
      Instant staleBefore
  ) {
    Objects.requireNonNull(keyId, "keyId");
    Objects.requireNonNull(usedAt, "usedAt");
    Objects.requireNonNull(staleBefore, "staleBefore");
    return context().write(connection -> SqliteSecurityTouchAccessKeyLastUsed.touchAccessKeyLastUsed(
        connection, keyId, usedAt, staleBefore
    ));
  }

  @Override
  default void appendAuditEvent(AuditEvent event) {
    Objects.requireNonNull(event, "event");
    context().write(connection -> {
      SqliteSecurityInsertAuditEvent.insertAuditEvent(connection, event);
      return null;
    });
  }

  @Override
  default List<AuditEvent> listAuditEvents(Ids.NovelId novelId) {
    Objects.requireNonNull(novelId, "novelId");
    return context().read(connection -> SqliteSecurityListAuditEvents.listAuditEvents(
        connection, novelId
    ));
  }
}
