package dev.storyblock.api.http;

import dev.storyblock.style.StyleAnalysisLeaseConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class ApiExceptionHandlerAnalysisLeaseConflictAction {
    static ResponseEntity<Map<String, Object>> analysisLeaseConflict(ApiExceptionHandler self, StyleAnalysisLeaseConflictException failure, HttpServletRequest request)  {
        return ApiExceptionHandlerResponse.response(request, new ApiFailureException(
                HttpStatus.PRECONDITION_FAILED,
                "ANALYSIS_LEASE_CONFLICT",
                "Analysis lease conflict",
                "analysis-lease-conflict",
                failure.getMessage(),
                Map.of("current_etag", failure.currentStatusHash()),
                null
        ));
    }
}
