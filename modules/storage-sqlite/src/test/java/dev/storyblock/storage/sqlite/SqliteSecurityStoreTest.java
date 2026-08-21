package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.security.AccessAuthenticationException;
import dev.storyblock.security.AccessKeyRequestConflictException;
import dev.storyblock.security.AccessKeyService;
import dev.storyblock.security.AccessScope;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditResult;
import dev.storyblock.security.IssueAccessKeyCommand;
import dev.storyblock.security.SecretAlreadyIssuedException;
import dev.storyblock.security.StoredAccessKey;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteSecurityStoreTest {
    private static final Instant NOW = Instant.parse("2026-08-21T12:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void opaqueKeysAreOneTimeNovelBoundAndNeverPersistPlaintext() throws Exception {
        Path path = temporaryDirectory.resolve("keys.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        String bearerToken;
        String encodedSecret;
        Ids.AccessKeyId keyId;
        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            AccessKeyService accessKeys = service(store);
            IssueAccessKeyCommand command = command(
                    genesis,
                    "issue-primary",
                    Set.of(AccessScope.NOVEL_READ, AccessScope.NOVEL_COMMIT),
                    NOW.plusSeconds(3600)
            );
            var issued = accessKeys.issue(command);
            bearerToken = issued.bearerToken();
            encodedSecret = bearerToken.substring(bearerToken.indexOf('.') + 1);
            keyId = issued.key().keyId();

            assertFalse(issued.toString().contains(bearerToken));
            assertTrue(issued.toString().contains("bearerToken=<redacted>"));
            assertTrue(bearerToken.matches(
                    "nv_key_[0-9a-f-]{36}\\.[A-Za-z0-9_-]{43}"
            ));
            assertEquals(genesis.novel().id(), issued.key().novelId());
            assertEquals(
                    Set.of(AccessScope.NOVEL_READ, AccessScope.NOVEL_COMMIT),
                    issued.key().scopes()
            );
            assertThrows(SecretAlreadyIssuedException.class, () -> accessKeys.issue(command));
            assertThrows(
                    AccessKeyRequestConflictException.class,
                    () -> accessKeys.issue(command(
                            genesis,
                            "issue-primary",
                            Set.of(AccessScope.NOVEL_READ),
                            NOW.plusSeconds(3600)
                    ))
            );

            StoredAccessKey stored = accessKeys.requireKey(keyId);
            assertEquals(32, stored.secretDigest().length);
            byte[] decodedSecret = Base64.getUrlDecoder().decode(encodedSecret);
            assertArrayEquals(hmac(decodedSecret), stored.secretDigest());
            assertFalse(Arrays.equals(
                    MessageDigest.getInstance("SHA-256").digest(decodedSecret),
                    stored.secretDigest()
            ));
            Arrays.fill(decodedSecret, (byte) 0);
            assertFalse(Arrays.equals(
                    stored.secretDigest(), encodedSecret.getBytes(StandardCharsets.US_ASCII)
            ));
            assertEquals(1, store.listAuditEvents(genesis.novel().id()).size());
            assertEquals("test-actor", accessKeys.authenticate(bearerToken, NOW).actorId());
            assertEquals(NOW, accessKeys.requireKey(keyId).lastUsedAt());
            accessKeys.authenticate(bearerToken, NOW.plusSeconds(299));
            assertEquals(NOW, accessKeys.requireKey(keyId).lastUsedAt());
            accessKeys.authenticate(bearerToken, NOW.plusSeconds(300));
            assertEquals(NOW.plusSeconds(300), accessKeys.requireKey(keyId).lastUsedAt());

            String changedToken = bearerToken.substring(0, bearerToken.length() - 1)
                    + (bearerToken.endsWith("A") ? "Q" : "A");
            assertThrows(
                    AccessAuthenticationException.class,
                    () -> accessKeys.authenticate(changedToken, NOW)
            );
        }

        byte[] databaseBytes = Files.readAllBytes(path);
        assertFalse(contains(databaseBytes, bearerToken.getBytes(StandardCharsets.US_ASCII)));
        assertFalse(contains(databaseBytes, encodedSecret.getBytes(StandardCharsets.US_ASCII)));
        try (SqliteDatabase database = SqliteDatabase.open(path)) {
            database.readOnly(connection -> {
                try (var statement = connection.prepareStatement("""
                        SELECT key_id, novel_id, secret_digest, scopes_json, actor_id,
                               issue_idempotency_key, issue_request_hash
                        FROM access_keys WHERE key_id = ?
                        """)) {
                    statement.setString(1, keyId.value());
                    try (var result = statement.executeQuery()) {
                        assertTrue(result.next());
                        assertEquals(32, result.getBytes("secret_digest").length);
                        String persistedText = result.getString("key_id")
                                + result.getString("novel_id")
                                + result.getString("scopes_json")
                                + result.getString("actor_id")
                                + result.getString("issue_idempotency_key")
                                + result.getString("issue_request_hash");
                        assertFalse(persistedText.contains(bearerToken));
                        assertFalse(persistedText.contains(encodedSecret));
                    }
                }
                return null;
            });
        }
    }

    @Test
    void expiredAndRevokedKeysFailClosedAndAuditRetentionIsIndependent() throws Exception {
        Path path = temporaryDirectory.resolve("lifecycle.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        Ids.AccessKeyId activeKeyId;
        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            AccessKeyService accessKeys = service(store);
            var expiring = accessKeys.issue(command(
                    genesis,
                    "issue-expiring",
                    Set.of(AccessScope.NOVEL_READ),
                    NOW.plusSeconds(60)
            ));
            assertThrows(
                    AccessAuthenticationException.class,
                    () -> accessKeys.authenticate(
                            expiring.bearerToken(), NOW.plusSeconds(60)
                    )
            );

            var active = accessKeys.issue(command(
                    genesis,
                    "issue-revoked",
                    Set.of(AccessScope.NOVEL_ADMIN),
                    NOW.plusSeconds(3600)
            ));
            activeKeyId = active.key().keyId();
            assertTrue(accessKeys.revoke(
                    activeKeyId,
                    genesis.novel().id(),
                    new AuditContext(
                            "req_revoke_first", "owner", null, NOW.plusSeconds(120)
                    )
            ));
            assertFalse(accessKeys.revoke(
                    activeKeyId,
                    genesis.novel().id(),
                    new AuditContext(
                            "req_revoke_retry", "owner", null, NOW.plusSeconds(121)
                    )
            ));
            assertThrows(
                    AccessAuthenticationException.class,
                    () -> accessKeys.authenticate(active.bearerToken(), NOW.plusSeconds(122))
            );

            var events = store.listAuditEvents(genesis.novel().id());
            assertEquals(4, events.size());
            assertEquals(2, events.stream()
                    .filter(event -> event.action() == AuditAction.ACCESS_KEY_ISSUE)
                    .count());
            assertEquals(AuditResult.SUCCEEDED, events.get(2).result());
            assertEquals(AuditResult.IDEMPOTENT, events.get(3).result());
            assertNotEquals(events.get(2).eventHash(), events.get(3).eventHash());
        }

        try (SqliteDatabase database = SqliteDatabase.open(path)) {
            assertEquals(Set.of(
                    "event_id", "occurred_at", "request_id", "actor_id",
                    "actor_key_id", "novel_id", "action", "subject_id",
                    "operation_id", "revision_id", "result", "operation_hash",
                    "content_hash", "event_hash"
            ), columns(database, "audit_events"));
            assertEquals(Set.of(
                    "key_id", "novel_id", "secret_digest", "scopes_json",
                    "actor_id", "created_at", "expires_at", "revoked_at",
                    "last_used_at", "issue_idempotency_key", "issue_request_hash"
            ), columns(database, "access_keys"));

            SQLException immutableAudit = assertThrows(SQLException.class, () ->
                    database.write(connection -> {
                        connection.createStatement().executeUpdate(
                                "UPDATE audit_events SET result = 'rejected'"
                        );
                        return null;
                    })
            );
            assertTrue(immutableAudit.getMessage().contains("append-only"));

            SQLException retainedKey = assertThrows(SQLException.class, () ->
                    database.write(connection -> {
                        try (var statement = connection.prepareStatement(
                                "DELETE FROM access_keys WHERE key_id = ?"
                        )) {
                            statement.setString(1, activeKeyId.value());
                            statement.executeUpdate();
                        }
                        return null;
                    })
            );
            assertTrue(retainedKey.getMessage().contains("retained for audit"));

            database.write(connection -> {
                connection.createStatement().executeUpdate("DELETE FROM audit_events");
                return null;
            });
            assertEquals(0, count(database, "audit_events"));
            assertEquals(2, count(database, "access_keys"));
        }
    }

    private static AccessKeyService service(SqliteRevisionStore store) {
        return new AccessKeyService(store, pepper());
    }

    private static IssueAccessKeyCommand command(
            RevisionManifest genesis,
            String idempotencyKey,
            Set<AccessScope> scopes,
            Instant expiresAt
    ) {
        return new IssueAccessKeyCommand(
                genesis.novel().id(),
                "test-actor",
                scopes,
                expiresAt,
                idempotencyKey,
                new AuditContext("req_" + idempotencyKey, "owner", null, NOW)
        );
    }

    private static long count(SqliteDatabase database, String table) throws SQLException {
        if (!Set.of("access_keys", "audit_events").contains(table)) {
            throw new IllegalArgumentException("Unexpected test table " + table);
        }
        return database.readOnly(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                result.next();
                return result.getLong(1);
            }
        });
    }

    private static Set<String> columns(SqliteDatabase database, String table)
            throws SQLException {
        if (!Set.of("access_keys", "audit_events").contains(table)) {
            throw new IllegalArgumentException("Unexpected test table " + table);
        }
        return database.readOnly(connection -> {
            Set<String> result = new LinkedHashSet<>();
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
                while (rows.next()) {
                    result.add(rows.getString("name"));
                }
            }
            return Set.copyOf(result);
        });
    }

    private static byte[] hmac(byte[] secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(pepper(), "HmacSHA256"));
        return mac.doFinal(secret);
    }

    private static byte[] pepper() {
        byte[] pepper = new byte[32];
        Arrays.fill(pepper, (byte) 0x5a);
        return pepper;
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        outer:
        for (int offset = 0; offset <= haystack.length - needle.length; offset++) {
            for (int index = 0; index < needle.length; index++) {
                if (haystack[offset + index] != needle[index]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
