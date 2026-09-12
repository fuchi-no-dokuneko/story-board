package dev.storyblock.storage.sqlite;

import dev.storyblock.rewrite.policy.RewriteCandidateReservation;

final class SqliteReservationData {
    record StoredReservation(
            String requestHash,
            RewriteCandidateReservation reservation
    ) {
    }
}
