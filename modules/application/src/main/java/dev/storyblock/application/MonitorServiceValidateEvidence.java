package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MonitorOutput;
import dev.storyblock.monitor.MonitorPacket;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class MonitorServiceValidateEvidence {
    static void validateEvidence(
            MonitorPacket packet,
            Set<Ids.BlockId> affected,
            MonitorOutput output
    ) {
        Map<Ids.BlockId, String> textByBlock = new LinkedHashMap<>();
        packet.renderPacket().blocks().forEach(block ->
                textByBlock.put(block.blockId(), block.text())
        );
        Set<Ids.BlockId> evidenced = new HashSet<>();
        output.evidence().forEach(evidence -> {
            String text = textByBlock.get(evidence.blockId());
            if (text == null || !evidence.matches(text)) {
                throw new IllegalArgumentException(
                        "Monitor evidence must match source text inside the packet window"
                );
            }
            evidenced.add(evidence.blockId());
        });
        if (!evidenced.equals(affected)) {
            throw new IllegalArgumentException(
                    "Every affected monitor block must have text evidence"
            );
        }
    }
}
