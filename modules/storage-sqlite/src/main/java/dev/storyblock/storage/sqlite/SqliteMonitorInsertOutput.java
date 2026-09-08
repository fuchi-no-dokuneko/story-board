package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.monitor.MonitorOutputKind;
import dev.storyblock.monitor.StoredMonitorRun;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteMonitorInsertOutput {
    static void insertOutput(Connection connection, StoredMonitorRun run)
            throws SQLException {
        String table;
        String idColumn;
        if (run.output().kind() == MonitorOutputKind.FINDING) {
            table = "monitor_issues";
            idColumn = "issue_id";
        } else {
            table = "monitor_proposed_operations";
            idColumn = "proposal_id";
        }
        String sql = "INSERT INTO " + table + "(" + idColumn
                + ", run_id, payload_json) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, run.outputId().value());
            statement.setString(2, run.runId().value());
            statement.setString(3, CanonicalJson.string(run.output().canonicalValue()));
            statement.executeUpdate();
        }
    }
}
