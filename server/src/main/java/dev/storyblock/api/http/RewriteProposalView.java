package dev.storyblock.api.http;

import dev.storyblock.rewrite.policy.RewriteCandidateReservation;
import java.util.*;

final class RewriteProposalView {
    private RewriteProposalView() {}
    static Map<String, Object> value(RewriteCandidateReservation reservation) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", reservation.eligibility().analysisId().value());
        value.put("cooldown_until", reservation.cooldownUntil().toString());
        value.put("finding_ids", reservation.eligibility().findingIds());
        value.put("novel_id", reservation.novelId().value());
        value.put("proposal_id", reservation.proposalId().value());
        value.put("reservation_hash", reservation.reservationHash());
        value.put("revision_hash", reservation.eligibility().revisionHash());
        value.put("revision_id", reservation.eligibility().revisionId().value());
        value.put("status", "pending");
        value.put("worker_input", reservation.workerInput().canonicalValue());
        return value;
    }
}
