package dev.storyblock.storage;

public final class StaleHeadException extends RuntimeException {
    public static final int HTTP_STATUS = 412;

    private final RevisionRef expected;
    private final RevisionRef actual;

    public StaleHeadException(RevisionRef expected, RevisionRef actual) {
        super("Expected head " + expected.revisionId().value() + " at " + expected.contentHash()
                + " but current head is " + actual.revisionId().value() + " at "
                + actual.contentHash());
        this.expected = expected;
        this.actual = actual;
    }

    public RevisionRef expected() {
        return expected;
    }

    public RevisionRef actual() {
        return actual;
    }
}
