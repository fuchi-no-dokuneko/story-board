package dev.storyblock.api.http;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecentReadLedgerTest {
    @Test void sharedReadsExpireAndRemainBoundToRevisionAndReturnedText() {
        var clock = new MovingClock();
        var ledger = new RecentReadLedger(clock);
        var firstReader = new ReadBodyRecorder(ledger);
        var anotherReader = new ReadBodyRecorder(ledger);
        firstReader.record(Map.of("novel_id", "nov_Ab123", "revision_id", "rev_Cd456",
                "scenes", List.of(Map.of("scene_id", "scn_Ef789", "block_count", 3))));
        assertFalse(ledger.fresh("nov_Ab123", "rev_Cd456", "blk_Aa123"));
        anotherReader.record(Map.of("novel_id", "nov_Ab123", "revision_id", "rev_Cd456",
                "blocks", List.of(Map.of("block_id", "blk_Aa123", "text", "夜雨停了。"))));
        assertTrue(ledger.fresh("nov_Ab123", "rev_Cd456", "blk_Aa123"));
        assertFalse(ledger.fresh("nov_Ab123", "rev_Cd456", "blk_Bb234"));
        assertFalse(ledger.fresh("nov_Ab123", "rev_Xy123", "blk_Aa123"));
        clock.now = clock.now.plusSeconds(300);
        assertTrue(ledger.fresh("nov_Ab123", "rev_Cd456", "blk_Aa123"));
        clock.now = clock.now.plusSeconds(1); ledger.prune();
        assertFalse(ledger.fresh("nov_Ab123", "rev_Cd456", "blk_Aa123"));
        firstReader.record(Map.of("novel_id", "nov_Ab123", "revision_id", "rev_Cd456",
                "scene_id", "scn_Ef789", "block_count", 0, "blocks", List.of()));
        assertTrue(ledger.fresh("nov_Ab123", "rev_Cd456", "scn_Ef789"));
    }

    static class MovingClock extends Clock {
        Instant now = Instant.parse("2026-09-12T12:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
