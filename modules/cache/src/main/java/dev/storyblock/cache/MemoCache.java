package dev.storyblock.cache;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Process-local LRU entries; concurrent requests for the same key share one load. */
public final class MemoCache<K, V> {
    private final int capacity;
    private final Map<K, CompletableFuture<V>> entries = new LinkedHashMap<>(16, .75f, true);
    private long hits, loads;

    public MemoCache(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Cache capacity must be positive");
        this.capacity = capacity;
    }

    public V get(K key, Supplier<V> loader) {
        Objects.requireNonNull(key); Objects.requireNonNull(loader);
        CompletableFuture<V> future;
        boolean owner;
        synchronized (entries) {
            future = entries.get(key); owner = future == null;
            if (owner) { future = new CompletableFuture<>(); entries.put(key, future); loads++; }
            else hits++;
        }
        if (owner) {
            try {
                future.complete(Objects.requireNonNull(loader.get()));
                trim();
            } catch (Throwable failure) {
                synchronized (entries) { entries.remove(key, future); }
                future.completeExceptionally(failure);
            }
        }
        try { return future.join(); }
        catch (CompletionException failure) {
            if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
            if (failure.getCause() instanceof Error error) throw error;
            throw failure;
        }
    }

    private void trim() {
        synchronized (entries) {
            var iterator = entries.entrySet().iterator();
            while (entries.size() > capacity && iterator.hasNext())
                if (iterator.next().getValue().isDone()) iterator.remove();
        }
    }

    public Snapshot snapshot() {
        synchronized (entries) { return new Snapshot(hits, loads, entries.size()); }
    }
    public record Snapshot(long hits, long loads, int entries) { }
}
