package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.*;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteReservationData.*;

final class SqliteReservationFindByIdempotency {
    static Optional<StoredReservation> findByIdempotency(
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
                        SqliteReservationParse.parse(result.getString("reservation_json"))
                )) : Optional.empty();
            }
        }
    }
}
