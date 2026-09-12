package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;

final class SqliteProfileData {
    enum MutationKind {
        CREATE_PROFILE("create_profile"),
        CREATE_VERSION("create_version"),
        TRANSITION("transition");

        final String canonicalName;

        MutationKind(String canonicalName) {
            this.canonicalName = canonicalName;
        }

        String canonicalName() {
            return canonicalName;
        }

        static MutationKind fromCanonical(String value) {
            for (MutationKind kind : values()) {
                if (kind.canonicalName.equals(value)) {
                    return kind;
                }
            }
            throw new StorageException("Unknown stored style mutation kind");
        }
    }

    record Mutation(
            MutationKind kind,
            String requestHash,
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId,
            Ids.StyleLifecycleEventId eventId
    ) {
    }
}
