package dev.storyblock.security;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

final class AccessKeyServiceIssueAction {
    static IssuedAccessKey issue(AccessKeyService self, IssueAccessKeyCommand command)  {
        Objects.requireNonNull(command, "command");
        Instant createdAt = command.auditContext().occurredAt();
        if (!command.expiresAt().isAfter(createdAt)) {
            throw new IllegalArgumentException("Access-key expiry must be in the future");
        }

        Ids.AccessKeyId keyId = Ids.AccessKeyId.create();
        byte[] secret = new byte[AccessKeyService.SECRET_BYTES];
        self.random.nextBytes(secret);
        String encodedSecret = AccessKeyService.ENCODER.encodeToString(secret);
        StoredAccessKey key;
        try {
            key = new StoredAccessKey(
                    keyId,
                    command.novelId(),
                    self.digest(secret),
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
        AccessKeyInsertResult result = self.store.issueAccessKey(
                key,
                command.idempotencyKey(),
                requestHash,
                command.auditContext()
        );
        if (result.idempotentReplay()) {
            throw new SecretAlreadyIssuedException();
        }
        return new IssuedAccessKey(
                result.key(), AccessKeyService.TOKEN_PREFIX + keyId.value() + "." + encodedSecret
        );
    }
}
