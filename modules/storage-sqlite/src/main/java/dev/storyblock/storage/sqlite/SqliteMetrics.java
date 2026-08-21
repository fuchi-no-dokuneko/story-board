package dev.storyblock.storage.sqlite;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

public final class SqliteMetrics {
    private final LongAdder connectionVerifications = new LongAdder();
    private final LongAdder readTransactions = new LongAdder();
    private final LongAdder writeAttempts = new LongAdder();
    private final LongAdder writeCommits = new LongAdder();
    private final LongAdder sqliteBusyTotal = new LongAdder();
    private final LongAdder writerWaitNanos = new LongAdder();
    private final LongAccumulator maxTransactionNanos = new LongAccumulator(Long::max, 0L);

    void recordConnectionVerification() {
        connectionVerifications.increment();
    }

    void recordRead(long transactionNanos) {
        readTransactions.increment();
        maxTransactionNanos.accumulate(transactionNanos);
    }

    void recordWriteAttempt() {
        writeAttempts.increment();
    }

    void recordWriteCommit(long waitNanos, long transactionNanos) {
        writeCommits.increment();
        writerWaitNanos.add(waitNanos);
        maxTransactionNanos.accumulate(transactionNanos);
    }

    void recordFailure(SQLException exception) {
        if (isBusy(exception)) {
            sqliteBusyTotal.increment();
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(
                connectionVerifications.sum(),
                readTransactions.sum(),
                writeAttempts.sum(),
                writeCommits.sum(),
                sqliteBusyTotal.sum(),
                nanosToMillis(writerWaitNanos.sum()),
                nanosToMillis(maxTransactionNanos.get())
        );
    }

    public static boolean isBusy(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLiteException sqliteException
                    && (sqliteException.getResultCode().code & 0xff)
                    == SQLiteErrorCode.SQLITE_BUSY.code) {
                return true;
            }
            if (current instanceof SQLException sqlException
                    && sqlException.getNextException() != null
                    && sqlException.getNextException() != current
                    && isBusy(sqlException.getNextException())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static long nanosToMillis(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    public record Snapshot(
            long connectionVerifications,
            long readTransactions,
            long writeAttempts,
            long writeCommits,
            long sqliteBusyTotal,
            long writerWaitMillis,
            long maxTransactionMillis
    ) {
    }
}
