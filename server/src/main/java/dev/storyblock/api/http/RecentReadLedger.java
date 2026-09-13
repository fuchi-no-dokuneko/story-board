package dev.storyblock.api.http;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public final class RecentReadLedger {
    private final Clock clock;
    private final ConcurrentHashMap<Key, Instant> reads = new ConcurrentHashMap<>();
    public RecentReadLedger(Clock clock) { this.clock = clock; }

    void mark(String novel, String revision, String item) {
        Instant now = clock.instant();
        reads.merge(new Key(novel, revision, item), now, (first, second) -> first.isAfter(second) ? first : second);
    }

    void prune() {
        Instant now = clock.instant();
        reads.entrySet().removeIf(e -> e.getValue().plusSeconds(300).isBefore(now));
    }

    boolean fresh(String novel, String revision, String item) {
        Instant read = reads.get(new Key(novel, revision, item));
        return read != null && !read.plusSeconds(300).isBefore(clock.instant());
    }

    private record Key(String novel, String revision, String item) {}
}
