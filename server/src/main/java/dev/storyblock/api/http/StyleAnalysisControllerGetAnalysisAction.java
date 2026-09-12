package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisResult;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;

final class StyleAnalysisControllerGetAnalysisAction {
    static ResponseEntity<Map<String, Object>> getAnalysis(StyleAnalysisController self, String analysisId)  {
        Ids.StyleAnalysisId id = new Ids.StyleAnalysisId(analysisId);
        StyleAnalysisJob job = self.analyses.getAnalysis(id);
        Map<String, Object> body = StyleAnalysisController.publicJob(job);
        Optional<StyleAnalysisResult> result = self.analyses.result(id);
        result.ifPresent(value -> {
            body.put("result", value.canonicalValue());
            body.put("trace_uri", "/v1/artifacts/" + value.traceArtifactId().value());
            body.put("windows_uri", "/v1/style-analyses/" + analysisId + "/windows");
        });
        return ResponseEntity.ok().eTag(job.statusHash()).body(body);
    }
}
