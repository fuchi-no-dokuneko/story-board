package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MonitorOutput;
import dev.storyblock.monitor.MonitorOutputKind;
import dev.storyblock.storage.StorageException;
import java.sql.*;

final class SqliteMonitorReadOutput {
    static MonitorOutput readOutput(
            Connection connection,
            Ids.MonitorRunId runId,
            Ids.MonitorOutputId outputId,
            MonitorOutputKind kind
    ) throws SQLException {
        String table = kind == MonitorOutputKind.FINDING
                ? "monitor_issues" : "monitor_proposed_operations";
        String idColumn = kind == MonitorOutputKind.FINDING
                ? "issue_id" : "proposal_id";
        String sql = "SELECT " + idColumn + ", payload_json FROM " + table
                + " WHERE run_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, runId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new StorageException(
                            "Stored monitor run is missing its output " + runId.value()
                    );
                }
                if (!outputId.value().equals(result.getString(idColumn))) {
                    throw new StorageException(
                            "Stored monitor output identity does not match its run"
                    );
                }
                return MonitorOutput.fromCanonical(SqliteMonitorParseObject.parseObject(
                        result.getString("payload_json"), "monitor output"
                ));
            }
        }
    }
}
