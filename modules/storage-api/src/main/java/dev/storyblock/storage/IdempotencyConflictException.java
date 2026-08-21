package dev.storyblock.storage;

public final class IdempotencyConflictException extends RuntimeException {
    public static final int HTTP_STATUS = 409;

    private final String idempotencyKey;
    private final String storedOperationHash;
    private final String attemptedOperationHash;

    public IdempotencyConflictException(
            String idempotencyKey,
            String storedOperationHash,
            String attemptedOperationHash
    ) {
        super("Idempotency key was already used with a different operation payload");
        this.idempotencyKey = idempotencyKey;
        this.storedOperationHash = storedOperationHash;
        this.attemptedOperationHash = attemptedOperationHash;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String storedOperationHash() {
        return storedOperationHash;
    }

    public String attemptedOperationHash() {
        return attemptedOperationHash;
    }
}
