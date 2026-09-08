package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteWorkerInput;
import dev.storyblock.security.AuditContext;
import java.util.Map;

final class RewriteCandidateReservationFromCanonicalFactory {
  static RewriteCandidateReservation fromCanonical(Map<String, Object> value)  {
    RewritePolicyCanonical.requireKeys(
        value, RewriteCandidateReservation.FIELDS, "rewrite_candidate_reservation"
    );
    if (!RewritePolicyModule.RESERVATION_SCHEMA_VERSION.equals(
        RewritePolicyCanonical.string(
            value, "schema_version", "rewrite_candidate_reservation"
        ))) {
      throw new IllegalArgumentException(
          "Rewrite reservation schema version is unsupported"
      );
    }
    Map<String, Object> audit = RewritePolicyCanonical.object(
        value.get("audit_context"),
        "rewrite_candidate_reservation.audit_context"
    );
    RewritePolicyCanonical.requireKeys(
        audit, RewriteCandidateReservation.AUDIT_FIELDS, "rewrite_candidate_reservation.audit_context"
    );
    String actorKeyId = RewritePolicyCanonical.optionalString(
        audit, "actor_key_id", "rewrite_candidate_reservation.audit_context"
    );
    RewriteCandidateReservation reservation = new RewriteCandidateReservation(
        RewriteEligibility.fromCanonical(RewritePolicyCanonical.object(
            value.get("eligibility"),
            "rewrite_candidate_reservation.eligibility"
        )),
        RewriteWorkerInput.fromCanonical(RewritePolicyCanonical.object(
            value.get("worker_input"),
            "rewrite_candidate_reservation.worker_input"
        )),
        new AuditContext(
            RewritePolicyCanonical.string(
                audit,
                "request_id",
                "rewrite_candidate_reservation.audit_context"
            ),
            RewritePolicyCanonical.string(
                audit,
                "actor_id",
                "rewrite_candidate_reservation.audit_context"
            ),
            actorKeyId == null ? null : new Ids.AccessKeyId(actorKeyId),
            RewritePolicyCanonical.instant(
                audit,
                "occurred_at",
                "rewrite_candidate_reservation.audit_context"
            )
        ),
        RewritePolicyCanonical.instant(
            value, "cooldown_until", "rewrite_candidate_reservation"
        )
    );
    String expected = RewritePolicyCanonical.string(
        value, "reservation_hash", "rewrite_candidate_reservation"
    );
    if (!RewriteCandidateReservation.HASH.matcher(expected).matches()
        || !reservation.reservationHash().equals(expected)) {
      throw new IllegalArgumentException("Rewrite reservation hash is invalid");
    }
    return reservation;
  }
}
