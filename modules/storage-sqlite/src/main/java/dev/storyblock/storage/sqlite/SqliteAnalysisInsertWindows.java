package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

final class SqliteAnalysisInsertWindows {
    static void insertWindows(
            Connection connection,
            Ids.StyleAnalysisId analysisId,
            List<StyleAnalysisWindowFinding> windows
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analysis_window_findings(
                    analysis_id, ordinal, window_id, decision_state,
                    can_trigger_rewrite, payload_json
                ) VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (StyleAnalysisWindowFinding window : windows) {
                statement.setString(1, analysisId.value());
                statement.setInt(2, window.ordinal());
                statement.setString(3, window.windowId());
                statement.setString(4, window.decisionState().canonicalName());
                statement.setBoolean(5, window.canTriggerRewrite());
                statement.setString(6, CanonicalJson.string(window.canonicalValue()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
