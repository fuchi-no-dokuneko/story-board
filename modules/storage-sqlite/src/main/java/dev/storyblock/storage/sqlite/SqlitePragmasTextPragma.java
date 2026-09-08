package dev.storyblock.storage.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class SqlitePragmasTextPragma {
    static String textPragma(Connection connection, String name) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA " + name)) {
            if (!result.next()) {
                throw new SQLException("PRAGMA " + name + " returned no row");
            }
            return result.getString(1);
        }
    }
}
