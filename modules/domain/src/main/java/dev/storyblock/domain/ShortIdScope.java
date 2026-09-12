package dev.storyblock.domain;

import java.util.HashSet;
import java.util.Set;

/** Per-operation allocator; storage implements durable reservations outside the domain. */
public final class ShortIdScope implements AutoCloseable {
    @FunctionalInterface
    public interface Allocator { String allocate(String prefix, String key); }
    private static final ThreadLocal<ShortIdScope> CURRENT = new ThreadLocal<>();
    private final ShortIdScope previous;
    private final Allocator allocator;
    private final Set<String> allocated = new HashSet<>();

    private ShortIdScope(Allocator allocator) {
        this.allocator = allocator; previous = CURRENT.get(); CURRENT.set(this);
    }
    public static ShortIdScope open(Allocator allocator) { return new ShortIdScope(allocator); }
    static String allocate(String prefix, String key) {
        var scope = CURRENT.get();
        if (scope == null) return null;
        String id = scope.allocator.allocate(prefix, key);
        scope.allocated.add(id);
        return id;
    }
    public static boolean owns(String id) {
        var scope = CURRENT.get();
        return scope != null && scope.allocated.contains(id);
    }
    public void close() {
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }
}
