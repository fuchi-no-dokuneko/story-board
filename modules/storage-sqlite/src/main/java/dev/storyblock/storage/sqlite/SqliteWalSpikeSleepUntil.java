package dev.storyblock.storage.sqlite;



final class SqliteWalSpikeSleepUntil {
    static void sleepUntil(long startAtMillis) throws InterruptedException {
        long delay = startAtMillis - System.currentTimeMillis();
        if (delay > 0) {
            Thread.sleep(delay);
        }
    }
}
