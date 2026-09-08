package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import java.util.Map;

final class SqliteReservationParse {
    static RewriteCandidateReservation parse(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> value = CanonicalJson.mapper().readValue(json, Map.class);
            return RewriteCandidateReservation.fromCanonical(value);
        } catch (RuntimeException failure) {
            throw new IllegalStateException(
                    "Stored rewrite reservation is not canonical", failure
            );
        }
    }
}
