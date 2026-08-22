package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.List;

public final class RewriteCooldownException extends RuntimeException {
    private final List<Ids.BlockId> blockedBlockIds;
    private final Instant cooldownUntil;

    public RewriteCooldownException(
            List<Ids.BlockId> blockedBlockIds,
            Instant cooldownUntil
    ) {
        super("An overlapping rewrite candidate is inside its cooldown");
        this.blockedBlockIds = List.copyOf(blockedBlockIds);
        this.cooldownUntil = cooldownUntil;
    }

    public List<Ids.BlockId> blockedBlockIds() {
        return blockedBlockIds;
    }

    public Instant cooldownUntil() {
        return cooldownUntil;
    }
}
