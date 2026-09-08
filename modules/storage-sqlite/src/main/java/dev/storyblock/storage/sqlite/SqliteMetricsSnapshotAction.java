package dev.storyblock.storage.sqlite;

import static dev.storyblock.storage.sqlite.SqliteMetrics.Snapshot;

final class SqliteMetricsSnapshotAction {
    static Snapshot snapshot(SqliteMetrics self)  {
        return new Snapshot(
                self.connectionVerifications.sum(),
                self.readTransactions.sum(),
                self.writeAttempts.sum(),
                self.writeCommits.sum(),
                self.sqliteBusyTotal.sum(),
                SqliteMetricsNanosToMillis.nanosToMillis(self.writerWaitNanos.sum()),
                SqliteMetricsNanosToMillis.nanosToMillis(self.maxTransactionNanos.get()),
                self.lastCheckpointMillis.get()
        );
    }
}
