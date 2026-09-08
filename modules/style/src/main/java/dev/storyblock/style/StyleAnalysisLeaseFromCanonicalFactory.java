package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.util.Map;

final class StyleAnalysisLeaseFromCanonicalFactory {
    static StyleAnalysisLease fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleAnalysisLease.FIELDS, "style_analysis_lease");
        return new StyleAnalysisLease(
                new Ids.JobId(StyleCanonical.string(
                        value, "job_id", "style_analysis_lease"
                )),
                new Ids.StyleAnalysisId(StyleCanonical.string(
                        value, "analysis_id", "style_analysis_lease"
                )),
                StyleAnalysisSnapshot.fromCanonical(StyleCanonical.object(
                        value.get("snapshot"), "style_analysis_lease.snapshot"
                )),
                StyleCanonical.string(value, "lease_owner", "style_analysis_lease"),
                StyleCanonical.integer(value, "attempt", "style_analysis_lease"),
                StyleCanonical.instant(value, "lease_until", "style_analysis_lease"),
                StyleCanonical.instant(
                        value, "retention_until", "style_analysis_lease"
                ),
                StyleCanonical.string(
                        value, "status_hash", "style_analysis_lease"
                ),
                StyleCanonical.bool(
                        value, "idempotent_replay", "style_analysis_lease"
                )
        );
    }
}
