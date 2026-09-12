package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StoredExportJob;
import dev.storyblock.storage.MissingExportJobException;
import java.util.Map;
import org.springframework.http.ResponseEntity;

final class CanonicalTransferControllerGetJobAction {
    static ResponseEntity<Map<String, Object>> getJob(CanonicalTransferController self, String jobId)  {
        Ids.JobId id = new Ids.JobId(jobId);
        final StoredExportJob job;
        try {
            job = self.transfers.getExportJob(id);
        } catch (MissingExportJobException missingExport) {
            var analysis = self.analyses.getJob(id);
            return ResponseEntity.ok()
                    .eTag(analysis.statusHash())
                    .body(StyleAnalysisController.publicJob(analysis));
        }
        String artifactUri = "/v1/artifacts/" + job.resultArtifactId().value();
        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("job_id", job.jobId().value()),
                Map.entry("novel_id", job.novelId().value()),
                Map.entry("revision_id", job.revision().revisionId().value()),
                Map.entry("kind", StoredExportJob.KIND),
                Map.entry("status", StoredExportJob.STATUS),
                Map.entry("attempt", StoredExportJob.ATTEMPT),
                Map.entry("result_artifact_id", job.resultArtifactId().value()),
                Map.entry("result_uri", artifactUri),
                Map.entry("created_at", job.createdAt().toString()),
                Map.entry("updated_at", job.createdAt().toString())
        ));
    }
}
