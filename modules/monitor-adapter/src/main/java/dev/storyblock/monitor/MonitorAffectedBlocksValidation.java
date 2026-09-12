package dev.storyblock.monitor;
import dev.storyblock.domain.Ids;
import java.util.*;
final class MonitorAffectedBlocksValidation {
  static void validate(List<MonitorBlockFingerprint> blocks, Ids.BlockId target) {
    if (blocks.isEmpty() || blocks.size() > 5) {
          throw new IllegalArgumentException("Monitor run requires 1 to 5 affected blocks");
        }
        if (new HashSet<>(blocks.stream()
            .map(MonitorBlockFingerprint::blockId).toList()).size()
            != blocks.size()) {
          throw new IllegalArgumentException("Monitor affected block IDs must be unique");
        }
        if (blocks.stream().noneMatch(block -> block.blockId().equals(target))) {
          throw new IllegalArgumentException("Monitor target must be an affected block");
        }
  }
}
