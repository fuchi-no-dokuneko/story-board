package dev.storyblock.storage.sqlite;



final class SqliteWalSpikeRunReader {
    static long runReader(SqliteDatabase database, int reads) throws Exception {
        long maximum = 0;
        for (int index = 0; index < reads; index++) {
            maximum = Math.max(maximum, SqliteWalSpikeCountRows.countRows(database));
            Thread.sleep(1L);
        }
        return maximum;
    }
}
