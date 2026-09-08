package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleAnalysisWindowSlice;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

final class SqliteAnalysisListWindows {
    static StyleAnalysisWindowSlice listWindows(
            Connection connection,
            Ids.StyleAnalysisId analysisId,
            int afterOrdinal,
            int limit
    ) throws SQLException {
        if (afterOrdinal < -1 || limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Style analysis page bounds are invalid");
        }
        SqliteAnalysisGetAnalysis.getAnalysis(connection, analysisId);
        List<StyleAnalysisWindowFinding> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT ordinal, window_id, decision_state, can_trigger_rewrite,
                       payload_json
                FROM analysis_window_findings
                WHERE analysis_id = ? AND ordinal > ?
                ORDER BY ordinal
                LIMIT ?
                """)) {
            statement.setString(1, analysisId.value());
            statement.setInt(2, afterOrdinal);
            statement.setInt(3, limit + 1);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    StyleAnalysisWindowFinding finding = StyleAnalysisWindowFinding
                            .fromCanonical(SqliteAnalysisParseObject.parseObject(
                                    result.getString("payload_json"),
                                    "style analysis window"
                            ));
                    if (finding.ordinal() != result.getInt("ordinal")
                            || !finding.windowId().equals(result.getString("window_id"))
                            || !finding.decisionState().canonicalName().equals(
                                    result.getString("decision_state")
                            )
                            || finding.canTriggerRewrite()
                            != result.getBoolean("can_trigger_rewrite")) {
                        throw new StorageException(
                                "Stored style analysis window integrity check failed"
                        );
                    }
                    values.add(finding);
                }
            }
        }
        Integer next = null;
        if (values.size() > limit) {
            values.removeLast();
            next = values.getLast().ordinal();
        }
        return new StyleAnalysisWindowSlice(values, next);
    }
}
