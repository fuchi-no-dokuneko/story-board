package dev.storyblock.storage.sqlite;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class SqliteAnalysisNullableString {
    static void nullableString(
            PreparedStatement statement,
            int index,
            String value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
