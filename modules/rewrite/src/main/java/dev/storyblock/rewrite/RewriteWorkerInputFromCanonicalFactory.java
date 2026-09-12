package dev.storyblock.rewrite;

import dev.storyblock.domain.Ids;
import java.util.Map;

final class RewriteWorkerInputFromCanonicalFactory {
    static RewriteWorkerInput fromCanonical(Map<String, Object> value)  {
        RewriteCanonical.requireKeys(value, RewriteWorkerInput.FIELDS, "rewrite_worker_input");
        if (!RewriteModule.INPUT_SCHEMA_VERSION.equals(RewriteCanonical.string(
                value, "schema_version", "rewrite_worker_input"
        ))) {
            throw new IllegalArgumentException("Rewrite input schema version is unsupported");
        }
        return new RewriteWorkerInput(
                new Ids.ProposalId(RewriteCanonical.string(
                        value, "proposal_id", "rewrite_worker_input"
                )),
                new Ids.StyleAnalysisId(RewriteCanonical.string(
                        value, "analysis_id", "rewrite_worker_input"
                )),
                new Ids.NovelId(RewriteCanonical.string(
                        value, "novel_id", "rewrite_worker_input"
                )),
                new Ids.RevisionId(RewriteCanonical.string(
                        value, "revision_id", "rewrite_worker_input"
                )),
                RewriteCanonical.string(value, "revision_hash", "rewrite_worker_input"),
                new Ids.StyleProfileVersionId(RewriteCanonical.string(
                        value, "profile_version_id", "rewrite_worker_input"
                )),
                RewriteCanonical.string(
                        value, "profile_version_hash", "rewrite_worker_input"
                ),
                RewriteCanonical.string(
                        value, "analyzer_contract_hash", "rewrite_worker_input"
                ),
                RewriteCanonical.string(
                        value, "window_configuration_hash", "rewrite_worker_input"
                ),
                RewriteCanonical.strings(
                        value.get("finding_ids"), "rewrite_worker_input.finding_ids"
                ),
                RewriteCanonical.objects(
                        value.get("blocks"), "rewrite_worker_input.blocks"
                ).stream().map(RewriteSourceBlock::fromCanonical).toList(),
                RewriteConstraints.fromCanonical(RewriteCanonical.object(
                        value.get("constraints"), "rewrite_worker_input.constraints"
                ))
        );
    }
}
