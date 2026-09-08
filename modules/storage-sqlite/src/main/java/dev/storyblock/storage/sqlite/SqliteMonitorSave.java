package dev.storyblock.storage.sqlite;

import dev.storyblock.monitor.MonitorSaveResult;
import dev.storyblock.monitor.StoredMonitorRun;
import dev.storyblock.storage.IdempotencyConflictException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

final class SqliteMonitorSave {
    static MonitorSaveResult save(Connection connection, StoredMonitorRun run)
            throws SQLException {
        Optional<StoredMonitorRun> prior = SqliteMonitorFindByIdempotencyKey.findByIdempotencyKey(
                connection, run.novelId(), run.idempotencyKey()
        );
        if (prior.isPresent()) {
            StoredMonitorRun stored = prior.get();
            if (!stored.requestHash().equals(run.requestHash())) {
                throw new IdempotencyConflictException(
                        run.idempotencyKey(), stored.requestHash(), run.requestHash()
                );
            }
            return new MonitorSaveResult(stored, true);
        }

        SqliteMonitorInsertRun.insert(connection, run);
        SqliteMonitorInsertOutput.insertOutput(connection, run);
        return new MonitorSaveResult(run, false);
    }
}
