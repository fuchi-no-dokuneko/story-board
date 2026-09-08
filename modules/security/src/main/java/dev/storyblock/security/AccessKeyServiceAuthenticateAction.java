package dev.storyblock.security;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import static dev.storyblock.security.AccessKeyService.ParsedCredential;

final class AccessKeyServiceAuthenticateAction {
    static AccessPrincipal authenticate(AccessKeyService self, String bearerToken, Instant now)  {
        Objects.requireNonNull(now, "now");
        ParsedCredential credential;
        try {
            credential = AccessKeyServiceParse.parse(bearerToken);
        } catch (RuntimeException failure) {
            throw new AccessAuthenticationException();
        }
        Optional<StoredAccessKey> found = self.store.findAccessKey(credential.keyId());
        byte[] actualDigest;
        try {
            actualDigest = self.digest(credential.secret());
        } finally {
            credential.clear();
        }
        byte[] expectedDigest = found
                .map(StoredAccessKey::secretDigest)
                .orElse(self.missingKeyDigest);
        boolean digestMatches = MessageDigest.isEqual(expectedDigest, actualDigest);
        Arrays.fill(actualDigest, (byte) 0);
        StoredAccessKey key = found.orElse(null);
        if (!digestMatches || key == null || !key.activeAt(now)) {
            throw new AccessAuthenticationException();
        }
        if (key.lastUsedAt() == null
                || !key.lastUsedAt().plus(AccessKeyService.LAST_USED_WRITE_INTERVAL).isAfter(now)) {
            self.store.touchAccessKeyLastUsed(
                    key.keyId(), now, now.minus(AccessKeyService.LAST_USED_WRITE_INTERVAL)
            );
        }
        return new AccessPrincipal(
                key.actorId(), key.keyId(), key.novelId(), key.scopes(), key.expiresAt(), false
        );
    }
}
