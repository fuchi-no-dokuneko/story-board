package dev.storyblock.storage;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.RevisionManifest;
import java.util.Objects;
import java.util.regex.Pattern;

public record CommitRequest(
        RevisionRef expectedHead,
        EditOperation operation,
        String operationHash,
        RevisionManifest candidate,
        String candidateHash
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public CommitRequest {
        Objects.requireNonNull(expectedHead, "expectedHead");
        Objects.requireNonNull(operation, "operation");
        requireHash(operationHash, "Operation hash");
        Objects.requireNonNull(candidate, "candidate");
        requireHash(candidateHash, "Candidate hash");
        if (!operation.context().baseRevisionId().equals(expectedHead.revisionId())) {
            throw new IllegalArgumentException("Operation base does not match expected head revision");
        }
        if (!operation.context().expectedHeadHash().equals(expectedHead.contentHash())) {
            throw new IllegalArgumentException("Operation hash guard does not match expected head hash");
        }
        if (!candidate.novel().id().equals(operation.context().novelId())) {
            throw new IllegalArgumentException("Candidate novel does not match the operation novel");
        }
        if (!expectedHead.revisionId().equals(candidate.parentId())) {
            throw new IllegalArgumentException("Candidate parent must be the expected head revision");
        }
    }

    private static void requireHash(String value, String field) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
    }
}
