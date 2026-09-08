package dev.storyblock.storage.sqlite;

import java.sql.SQLException;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

public final class SqliteMetrics {
    final LongAdder connectionVerifications = new LongAdder();
    final LongAdder readTransactions = new LongAdder();
    final LongAdder writeAttempts = new LongAdder();
    final LongAdder writeCommits = new LongAdder();
    final LongAdder sqliteBusyTotal = new LongAdder();
    final LongAdder writerWaitNanos = new LongAdder();
    final LongAccumulator maxTransactionNanos = new LongAccumulator(Long::max, 0L);
    final AtomicLong lastCheckpointMillis = new AtomicLong();

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

    void recordCheckpoint(long durationMillis) {
        lastCheckpointMillis.set(durationMillis);
    }

    public Snapshot snapshot() {
        return SqliteMetricsSnapshotAction.snapshot(this);
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

    public record Snapshot(
            long connectionVerifications,
            long readTransactions,
            long writeAttempts,
            long writeCommits,
            long sqliteBusyTotal,
            long writerWaitMillis,
            long maxTransactionMillis,
            long lastCheckpointMillis
    ) {
    }
}
