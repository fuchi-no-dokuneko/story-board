package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.MissingRewriteReservationException;
import dev.storyblock.rewrite.policy.ReserveRewriteCandidateCommand;
import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import dev.storyblock.rewrite.policy.RewriteCandidateReservationSaveResult;
import dev.storyblock.rewrite.policy.RewriteCooldownException;
import dev.storyblock.rewrite.policy.RewriteFindingAlreadyReservedException;
import dev.storyblock.rewrite.policy.RewriteReservationConflictException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class SqliteRewriteReservationStore {
    private SqliteRewriteReservationStore() {
    }

    static RewriteCandidateReservationSaveResult reserve(
            Connection connection,
            ReserveRewriteCandidateCommand command
    ) throws SQLException {
        RewriteCandidateReservation candidate = command.reservation();
        Optional<StoredReservation> prior = findByIdempotency(
                connection, candidate.novelId(), command.idempotencyKey()
        );
        if (prior.isPresent()) {
            if (!prior.get().requestHash().equals(command.requestHash())) {
                throw new RewriteReservationConflictException();
            }
            return new RewriteCandidateReservationSaveResult(
                    prior.get().reservation(), true
            );
        }
        requireUnreservedFindings(connection, candidate);
        requireCooldownAvailable(connection, candidate);
        insertReservation(connection, command);
        insertFindings(connection, candidate);
        insertBlocks(connection, candidate);
        return new RewriteCandidateReservationSaveResult(candidate, false);
    }

    static RewriteCandidateReservation get(
            Connection connection,
            Ids.ProposalId proposalId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT reservation_json
                FROM rewrite_candidate_reservations
                WHERE proposal_id = ?
                """)) {
            statement.setString(1, proposalId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingRewriteReservationException(proposalId);
                }
                return parse(result.getString(1));
            }
        }
    }

    private static Optional<StoredReservation> findByIdempotency(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT request_hash, reservation_json
                FROM rewrite_candidate_reservations
                WHERE novel_id = ? AND idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(new StoredReservation(
                        result.getString("request_hash"),
                        parse(result.getString("reservation_json"))
                )) : Optional.empty();
            }
        }
    }

    private static void requireUnreservedFindings(
            Connection connection,
            RewriteCandidateReservation reservation
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT proposal_id
                FROM rewrite_reserved_findings
                WHERE analysis_id = ? AND finding_id = ?
                """)) {
            for (String findingId : reservation.eligibility().findingIds()) {
                statement.setString(1, reservation.eligibility().analysisId().value());
                statement.setString(2, findingId);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        throw new RewriteFindingAlreadyReservedException(
                                findingId, new Ids.ProposalId(result.getString(1))
                        );
                    }
                }
            }
        }
    }

    private static void requireCooldownAvailable(
            Connection connection,
            RewriteCandidateReservation reservation
    ) throws SQLException {
        String placeholders = String.join(",", java.util.Collections.nCopies(
                reservation.eligibility().affectedBlockIds().size(), "?"
        ));
        String sql = """
                SELECT DISTINCT blocks.block_id, reservations.cooldown_until
                FROM rewrite_reserved_blocks blocks
                JOIN rewrite_candidate_reservations reservations
                  ON reservations.proposal_id = blocks.proposal_id
                WHERE blocks.novel_id = ?
                  AND julianday(reservations.cooldown_until) > julianday(?)
                  AND blocks.block_id IN (%s)
                ORDER BY blocks.block_id
                """.formatted(placeholders);
        List<Ids.BlockId> blocked = new ArrayList<>();
        java.time.Instant latest = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reservation.novelId().value());
            statement.setString(2, reservation.createdAt().toString());
            int parameter = 3;
            for (Ids.BlockId blockId : reservation.eligibility().affectedBlockIds()) {
                statement.setString(parameter++, blockId.value());
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    blocked.add(new Ids.BlockId(result.getString("block_id")));
                    java.time.Instant until = java.time.Instant.parse(
                            result.getString("cooldown_until")
                    );
                    if (latest == null || until.isAfter(latest)) {
                        latest = until;
                    }
                }
            }
        }
        if (!blocked.isEmpty()) {
            throw new RewriteCooldownException(blocked, latest);
        }
    }

    private static void insertReservation(
            Connection connection,
            ReserveRewriteCandidateCommand command
    ) throws SQLException {
        RewriteCandidateReservation value = command.reservation();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rewrite_candidate_reservations(
                    proposal_id, novel_id, analysis_id, revision_id, revision_hash,
                    profile_version_id, eligibility_hash, worker_input_hash,
                    reservation_hash, idempotency_key, request_hash,
                    reservation_json, created_at, cooldown_until
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, value.proposalId().value());
            statement.setString(2, value.novelId().value());
            statement.setString(3, value.eligibility().analysisId().value());
            statement.setString(4, value.eligibility().revisionId().value());
            statement.setString(5, value.eligibility().revisionHash());
            statement.setString(6, value.eligibility().profileVersionId().value());
            statement.setString(7, value.eligibility().eligibilityHash());
            statement.setString(8, value.workerInput().inputHash());
            statement.setString(9, value.reservationHash());
            statement.setString(10, command.idempotencyKey());
            statement.setString(11, command.requestHash());
            statement.setString(12, CanonicalJson.string(value.canonicalValue()));
            statement.setString(13, value.createdAt().toString());
            statement.setString(14, value.cooldownUntil().toString());
            statement.executeUpdate();
        }
    }

    private static void insertFindings(
            Connection connection,
            RewriteCandidateReservation value
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rewrite_reserved_findings(
                    analysis_id, finding_id, proposal_id
                ) VALUES (?, ?, ?)
                """)) {
            for (String findingId : value.eligibility().findingIds()) {
                statement.setString(1, value.eligibility().analysisId().value());
                statement.setString(2, findingId);
                statement.setString(3, value.proposalId().value());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertBlocks(
            Connection connection,
            RewriteCandidateReservation value
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rewrite_reserved_blocks(proposal_id, novel_id, block_id)
                VALUES (?, ?, ?)
                """)) {
            for (Ids.BlockId blockId : value.eligibility().affectedBlockIds()) {
                statement.setString(1, value.proposalId().value());
                statement.setString(2, value.novelId().value());
                statement.setString(3, blockId.value());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static RewriteCandidateReservation parse(String json) {
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

    private record StoredReservation(
            String requestHash,
            RewriteCandidateReservation reservation
    ) {
    }
}
