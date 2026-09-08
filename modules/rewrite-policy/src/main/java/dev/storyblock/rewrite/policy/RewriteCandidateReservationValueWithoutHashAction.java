package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class RewriteCandidateReservationValueWithoutHashAction {
    static Map<String, Object> valueWithoutHash(RewriteCandidateReservation self)  {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("actor_id", self.auditContext().actorId());
        audit.put("actor_key_id", self.auditContext().actorKeyId() == null
                ? null : self.auditContext().actorKeyId().value());
        audit.put("occurred_at", self.auditContext().occurredAt().toString());
        audit.put("request_id", self.auditContext().requestId());
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("audit_context", audit);
        value.put("cooldown_until", self.cooldownUntil().toString());
        value.put("eligibility", self.eligibility().canonicalValue());
        value.put("schema_version", RewritePolicyModule.RESERVATION_SCHEMA_VERSION);
        value.put("worker_input", self.workerInput().canonicalValue());
        return CanonicalValues.freezeMap(value, "rewrite_candidate_reservation_content");
    }
}
