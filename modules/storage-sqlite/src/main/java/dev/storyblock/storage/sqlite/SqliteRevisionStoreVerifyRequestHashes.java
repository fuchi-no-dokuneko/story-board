package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.storage.CommitRequest;

final class SqliteRevisionStoreVerifyRequestHashes {
    static void verifyRequestHashes(CommitRequest request) {
        String operationHash = EditOperationCanonicalMapper.hash(request.operation());
        if (!operationHash.equals(request.operationHash())) {
            throw new IllegalArgumentException("Commit operation hash does not match canonical payload");
        }
        String candidateHash = NarrativeCanonicalMapper.toCanonical(request.candidate()).contentHash();
        if (!candidateHash.equals(request.candidateHash())) {
            throw new IllegalArgumentException("Commit candidate hash does not match canonical content");
        }
    }
}
