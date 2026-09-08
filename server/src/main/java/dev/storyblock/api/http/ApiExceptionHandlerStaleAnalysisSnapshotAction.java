package dev.storyblock.api.http;

import dev.storyblock.style.StyleAnalysisSnapshotConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class ApiExceptionHandlerStaleAnalysisSnapshotAction {
    static ResponseEntity<Map<String, Object>> staleAnalysisSnapshot(ApiExceptionHandler self, StyleAnalysisSnapshotConflictException failure, HttpServletRequest request)  {
        return ApiExceptionHandlerResponse.response(request, new ApiFailureException(
                HttpStatus.PRECONDITION_FAILED,
                "ANALYSIS_SNAPSHOT_CONFLICT",
                "Analysis snapshot conflict",
                "analysis-snapshot-conflict",
                failure.getMessage(),
                Map.of("current_etag", failure.currentRevisionHash()),
                null
        ));
    }
}
