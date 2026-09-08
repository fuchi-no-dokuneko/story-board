package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

final class SqliteTransferPutPortableArtifact {
    static PortableArtifactPutResult putPortableArtifact(
            Connection connection,
            PortableArtifactPutRequest request
    ) throws SQLException {
        Optional<StoredArtifact> prior = SqliteTransferFindArtifact.findArtifact(
                connection, request.artifact().artifactId()
        );
        if (prior.isPresent()) {
            StoredArtifact stored = prior.get();
            if (!SqliteTransferSamePortableArtifact.samePortableArtifact(stored, request.artifact())) {
                throw new IdempotencyConflictException(
                        request.idempotencyKey(),
                        stored.contentHash(),
                        request.artifact().contentHash()
                );
            }
            return new PortableArtifactPutResult(stored, true);
        }

        RevisionRef actualHead = SqliteTransferRequireHead.requireHead(connection, request.artifact().novelId());
        if (!actualHead.equals(request.expectedHead())) {
            throw new StaleHeadException(request.expectedHead(), actualHead);
        }
        SqliteTransferInsertArtifact.insertArtifact(connection, request.artifact());
        return new PortableArtifactPutResult(request.artifact(), false);
    }
}
