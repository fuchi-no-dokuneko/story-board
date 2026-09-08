package dev.storyblock.security;

import dev.storyblock.domain.Ids;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AccessKeyService {
    public static final Duration LAST_USED_WRITE_INTERVAL = Duration.ofMinutes(5);
    public static final int SECRET_BYTES = 32;

    static final String TOKEN_PREFIX = "nv_";
    static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    final AccessKeyStore store;
    final byte[] pepper;
    final SecureRandom random;
    final byte[] missingKeyDigest;

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
        return AccessKeyServiceIssueAction.issue(this, command);
    }

    public AccessPrincipal authenticate(String bearerToken, Instant now) {
        return AccessKeyServiceAuthenticateAction.authenticate(this, bearerToken, now);
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

    byte[] digest(byte[] secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return mac.doFinal(secret);
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("HmacSHA256 is unavailable", failure);
        }
    }

    record ParsedCredential(Ids.AccessKeyId keyId, byte[] secret) {
        ParsedCredential {
            Objects.requireNonNull(keyId, "keyId");
            Objects.requireNonNull(secret, "secret");
        }

        @Override
        public byte[] secret() {
            return secret;
        }

        void clear() {
            Arrays.fill(secret, (byte) 0);
        }
    }
}
