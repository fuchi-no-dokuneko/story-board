package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteDatabaseTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void requiredPragmasAreVerifiedOnEveryPhysicalConnection() throws Exception {
        Path databasePath = temporaryDirectory.resolve("pragmas.db");
        try (SqliteDatabase database = SqliteDatabase.open(databasePath)) {
            List<Connection> borrowed = new ArrayList<>();
            try {
                for (int index = 0; index < SqliteSettings.REQUIRED_MAXIMUM_POOL_SIZE; index++) {
                    borrowed.add(database.borrowConnection());
                }
                for (Connection connection : borrowed) {
                    SqlitePragmas state = SqlitePragmas.read(connection);
                    assertEquals("wal", state.journalMode());
                    assertEquals(2, state.synchronous());
                    assertTrue(state.foreignKeys());
                    assertEquals(5_000, state.busyTimeoutMillis());
                    assertTrue(state.explicitReadOnly());
                    assertFalse(state.loadExtensionsEnabled());
                }
            } finally {
                for (Connection connection : borrowed) {
                    connection.close();
                }
            }
            assertEquals(4, database.metrics().connectionVerifications());
        }
    }

    @Test
    void explicitReadOnlyTransactionRejectsWritesAndPoolStateIsReset() throws Exception {
        Path databasePath = temporaryDirectory.resolve("readonly.db");
        try (SqliteDatabase database = SqliteDatabase.open(databasePath)) {
            database.write(connection -> {
                connection.createStatement().execute("CREATE TABLE notes (id INTEGER PRIMARY KEY)");
                return null;
            });

            SQLException readOnlyFailure = assertThrows(SQLException.class, () ->
                    database.readOnly(connection -> {
                        connection.createStatement().executeUpdate("INSERT INTO notes(id) VALUES (1)");
                        return null;
                    })
            );
            assertFalse(SqliteMetrics.isBusy(readOnlyFailure));

            database.write(connection -> {
                connection.createStatement().executeUpdate("INSERT INTO notes(id) VALUES (2)");
                return null;
            });
            assertFalse(database.inspectPragmas().queryOnly());
            assertEquals(1L, countRows(database, "notes"));
            assertEquals(1L, countRows(database, "notes"));
        }
    }

    @Test
    void controlledWriterContentionProducesObservableBusyMetric() throws Exception {
        Path databasePath = temporaryDirectory.resolve("busy.db");
        SqliteSettings probeSettings = new SqliteSettings(1, 25);
        try (SqliteDatabase holder = SqliteDatabase.open(databasePath, probeSettings);
             SqliteDatabase contender = SqliteDatabase.open(databasePath, probeSettings);
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            holder.write(connection -> {
                connection.createStatement().execute("CREATE TABLE writes (id INTEGER PRIMARY KEY)");
                return null;
            });

            CountDownLatch lockHeld = new CountDownLatch(1);
            CountDownLatch releaseLock = new CountDownLatch(1);
            Future<Void> holdingWrite = executor.submit(() -> holder.write(connection -> {
                connection.createStatement().executeUpdate("INSERT INTO writes(id) VALUES (1)");
                lockHeld.countDown();
                await(releaseLock);
                return null;
            }));

            assertTrue(lockHeld.await(5, TimeUnit.SECONDS));
            try {
                SQLException busy = assertThrows(SQLException.class, () ->
                        contender.write(connection -> {
                            connection.createStatement().executeUpdate(
                                    "INSERT INTO writes(id) VALUES (2)"
                            );
                            return null;
                        })
                );
                assertTrue(SqliteMetrics.isBusy(busy));
                assertEquals(1, contender.metrics().sqliteBusyTotal());
            } finally {
                releaseLock.countDown();
            }
            holdingWrite.get(5, TimeUnit.SECONDS);
            assertEquals(1L, countRows(holder, "writes"));
        }
    }

    private static long countRows(SqliteDatabase database, String table) throws SQLException {
        if (!table.equals("notes") && !table.equals("writes")) {
            throw new IllegalArgumentException("Unexpected test table");
        }
        return database.readOnly(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                result.next();
                return result.getLong(1);
            }
        });
    }

    private static void await(CountDownLatch latch) throws SQLException {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new SQLException("Timed out waiting for contention probe release");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted during contention probe", exception);
        }
    }
}
