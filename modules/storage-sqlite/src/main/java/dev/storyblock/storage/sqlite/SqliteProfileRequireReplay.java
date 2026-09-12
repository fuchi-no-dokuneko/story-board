package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.IdempotencyConflictException;
import static dev.storyblock.storage.sqlite.SqliteProfileData.*;

final class SqliteProfileRequireReplay {
    static Mutation requireReplay(
            Mutation mutation,
            MutationKind kind,
            String requestHash,
            String idempotencyKey
    ) {
        if (mutation.kind() != kind || !mutation.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    idempotencyKey, mutation.requestHash(), requestHash
            );
        }
        return mutation;
    }
}
