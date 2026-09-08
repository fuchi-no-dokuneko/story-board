package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteWorkerInput;
import dev.storyblock.security.AuditContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import static dev.storyblock.rewrite.policy.RewriteCandidateReservation.*;

final class RewriteCandidateReservationValidation {
  static void validate(RewriteEligibility eligibility, RewriteWorkerInput workerInput, AuditContext auditContext, Instant cooldownUntil) {
    Objects.requireNonNull(eligibility, "eligibility");
        Objects.requireNonNull(workerInput, "workerInput");
        Objects.requireNonNull(auditContext, "auditContext");
        Objects.requireNonNull(cooldownUntil, "cooldownUntil");
        Duration cooldown = Duration.between(
            auditContext.occurredAt(), cooldownUntil
        );
        if (cooldown.compareTo(RewritePolicyModule.MIN_COOLDOWN) < 0
            || cooldown.compareTo(RewritePolicyModule.MAX_COOLDOWN) > 0) {
          throw new IllegalArgumentException("Rewrite cooldown duration is invalid");
        }
        if (!workerInput.analysisId().equals(eligibility.analysisId())
            || !workerInput.novelId().equals(eligibility.novelId())
            || !workerInput.revisionId().equals(eligibility.revisionId())
            || !workerInput.revisionHash().equals(eligibility.revisionHash())
            || !workerInput.profileVersionId().equals(
                eligibility.profileVersionId()
            )
            || !workerInput.profileVersionHash().equals(
                eligibility.profileVersionHash()
            )
            || !workerInput.analyzerContractHash().equals(
                eligibility.analyzerContractHash()
            )
            || !workerInput.windowConfigurationHash().equals(
                eligibility.windowConfigurationHash()
            )
            || !workerInput.findingIds().equals(eligibility.findingIds())) {
          throw new IllegalArgumentException(
              "Rewrite worker input does not match its eligibility binding"
          );
        }
        List<Ids.BlockId> editable = workerInput.blocks().stream()
            .filter(block -> block.editable())
            .map(block -> block.blockId())
            .toList();
        if (!editable.equals(eligibility.affectedBlockIds())) {
          throw new IllegalArgumentException(
              "Rewrite worker input editable range does not match eligibility"
          );
        }
  }
}
