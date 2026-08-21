package dev.storyblock.security;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AccessKeyService {
    public static final Duration LAST_USED_WRITE_INTERVAL = Duration.ofMinutes(5);
    public static final int SECRET_BYTES = 32;

    private static final String TOKEN_PREFIX = "nv_";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final AccessKeyStore store;
    private final byte[] pepper;
    private final SecureRandom random;
    private final byte[] missingKeyDigest;

    public AccessKeyService(AccessKeyStore store, byte[] pepper) {
        this(store, pepper, new SecureRandom());
    }

    AccessKeyService(AccessKeyStore store, byte[] pepper, SecureRandom random) {
        this.store = Objects.requireNonNull(store, "store");
        this.pepper = Objects.requireNonNull(pepper, "pepper").clone();
        if (this.pepper.length < SECRET_BYTES) {
            throw new IllegalArgumentException("Server pepper must contain at least 256 bits");
        }
        this.random = Objects.requireNonNull(random, "random");
        this.missingKeyDigest = digest(new byte[SECRET_BYTES]);
    }

    public IssuedAccessKey issue(IssueAccessKeyCommand command) {
        Objects.requireNonNull(command, "command");
        Instant createdAt = command.auditContext().occurredAt();
        if (!command.expiresAt().isAfter(createdAt)) {
            throw new IllegalArgumentException("Access-key expiry must be in the future");
        }

        Ids.AccessKeyId keyId = Ids.AccessKeyId.create();
        byte[] secret = new byte[SECRET_BYTES];
        random.nextBytes(secret);
        String encodedSecret = ENCODER.encodeToString(secret);
        StoredAccessKey key;
        try {
            key = new StoredAccessKey(
                    keyId,
                    command.novelId(),
                    digest(secret),
                    command.scopes(),
                    command.actorId(),
                    createdAt,
                    command.expiresAt(),
                    null,
                    null
            );
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
        String requestHash = CanonicalJson.hash(Map.of(
                "novel_id", command.novelId().value(),
                "actor_id", command.actorId(),
                "scopes", AccessScope.canonicalNames(command.scopes()),
                "expires_at", command.expiresAt().toString()
        ));
        AccessKeyInsertResult result = store.issueAccessKey(
                key,
                command.idempotencyKey(),
                requestHash,
                command.auditContext()
        );
        if (result.idempotentReplay()) {
            throw new SecretAlreadyIssuedException();
        }
        return new IssuedAccessKey(
                result.key(), TOKEN_PREFIX + keyId.value() + "." + encodedSecret
        );
    }

    public AccessPrincipal authenticate(String bearerToken, Instant now) {
        Objects.requireNonNull(now, "now");
        ParsedCredential credential;
        try {
            credential = parse(bearerToken);
        } catch (RuntimeException failure) {
            throw new AccessAuthenticationException();
        }
        Optional<StoredAccessKey> found = store.findAccessKey(credential.keyId());
        byte[] actualDigest;
        try {
            actualDigest = digest(credential.secret());
        } finally {
            credential.clear();
        }
        byte[] expectedDigest = found
                .map(StoredAccessKey::secretDigest)
                .orElse(missingKeyDigest);
        boolean digestMatches = MessageDigest.isEqual(expectedDigest, actualDigest);
        Arrays.fill(actualDigest, (byte) 0);
        StoredAccessKey key = found.orElse(null);
        if (!digestMatches || key == null || !key.activeAt(now)) {
            throw new AccessAuthenticationException();
        }
        if (key.lastUsedAt() == null
                || !key.lastUsedAt().plus(LAST_USED_WRITE_INTERVAL).isAfter(now)) {
            store.touchAccessKeyLastUsed(
                    key.keyId(), now, now.minus(LAST_USED_WRITE_INTERVAL)
            );
        }
        return new AccessPrincipal(
                key.actorId(), key.keyId(), key.novelId(), key.scopes(), key.expiresAt(), false
        );
    }

    public StoredAccessKey requireKey(Ids.AccessKeyId keyId) {
        return store.findAccessKey(keyId)
                .orElseThrow(() -> new MissingAccessKeyException(keyId));
    }

    public boolean revoke(
            Ids.AccessKeyId keyId,
            Ids.NovelId expectedNovelId,
            AuditContext context
    ) {
        return store.revokeAccessKey(keyId, expectedNovelId, context);
    }

    private byte[] digest(byte[] secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return mac.doFinal(secret);
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("HmacSHA256 is unavailable", failure);
        }
    }

    private static ParsedCredential parse(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            throw new IllegalArgumentException("Invalid bearer token");
        }
        int separator = token.indexOf('.', TOKEN_PREFIX.length());
        if (separator < 0 || token.indexOf('.', separator + 1) >= 0) {
            throw new IllegalArgumentException("Invalid bearer token");
        }
        Ids.AccessKeyId keyId = new Ids.AccessKeyId(
                token.substring(TOKEN_PREFIX.length(), separator)
        );
        String encoded = token.substring(separator + 1);
        byte[] secret = DECODER.decode(encoded.getBytes(StandardCharsets.US_ASCII));
        if (secret.length != SECRET_BYTES || !ENCODER.encodeToString(secret).equals(encoded)) {
            Arrays.fill(secret, (byte) 0);
            throw new IllegalArgumentException("Invalid bearer token");
        }
        return new ParsedCredential(keyId, secret);
    }

    private record ParsedCredential(Ids.AccessKeyId keyId, byte[] secret) {
        private ParsedCredential {
            Objects.requireNonNull(keyId, "keyId");
            Objects.requireNonNull(secret, "secret");
        }

        @Override
        public byte[] secret() {
            return secret;
        }

        private void clear() {
            Arrays.fill(secret, (byte) 0);
        }
    }
}
