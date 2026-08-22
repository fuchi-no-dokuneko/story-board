package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteWorkerInput;
import dev.storyblock.security.AuditContext;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record RewriteCandidateReservation(
        RewriteEligibility eligibility,
        RewriteWorkerInput workerInput,
        AuditContext auditContext,
        Instant cooldownUntil
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
            "audit_context", "cooldown_until", "eligibility", "reservation_hash",
            "schema_version", "worker_input"
    );
    private static final Set<String> AUDIT_FIELDS = Set.of(
            "actor_id", "actor_key_id", "occurred_at", "request_id"
    );

    public RewriteCandidateReservation {
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

    public static RewriteCandidateReservation fromCanonical(
            Map<String, Object> value
    ) {
        RewritePolicyCanonical.requireKeys(
                value, FIELDS, "rewrite_candidate_reservation"
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
                audit, AUDIT_FIELDS, "rewrite_candidate_reservation.audit_context"
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
        if (!HASH.matcher(expected).matches()
                || !reservation.reservationHash().equals(expected)) {
            throw new IllegalArgumentException("Rewrite reservation hash is invalid");
        }
        return reservation;
    }

    public Ids.ProposalId proposalId() {
        return workerInput.proposalId();
    }

    public Ids.NovelId novelId() {
        return eligibility.novelId();
    }

    public Instant createdAt() {
        return auditContext.occurredAt();
    }

    public String reservationHash() {
        return CanonicalJson.hash(valueWithoutHash());
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>(valueWithoutHash());
        value.put("reservation_hash", reservationHash());
        return CanonicalValues.freezeMap(value, "rewrite_candidate_reservation");
    }

    private Map<String, Object> valueWithoutHash() {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("actor_id", auditContext.actorId());
        audit.put("actor_key_id", auditContext.actorKeyId() == null
                ? null : auditContext.actorKeyId().value());
        audit.put("occurred_at", auditContext.occurredAt().toString());
        audit.put("request_id", auditContext.requestId());
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("audit_context", audit);
        value.put("cooldown_until", cooldownUntil.toString());
        value.put("eligibility", eligibility.canonicalValue());
        value.put("schema_version", RewritePolicyModule.RESERVATION_SCHEMA_VERSION);
        value.put("worker_input", workerInput.canonicalValue());
        return CanonicalValues.freezeMap(value, "rewrite_candidate_reservation_content");
    }
}
