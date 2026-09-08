package dev.storyblock.api.http;

import dev.storyblock.style.StyleAnalysisResultConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class ApiExceptionHandlerAnalysisResultConflictAction {
    static ResponseEntity<Map<String, Object>> analysisResultConflict(ApiExceptionHandler self, StyleAnalysisResultConflictException failure, HttpServletRequest request)  {
        return ApiExceptionHandlerResponse.response(request, new ApiFailureException(
                HttpStatus.CONFLICT,
                "ANALYSIS_RESULT_CONFLICT",
                "Analysis result conflict",
                "analysis-result-conflict",
                failure.getMessage(),
                Map.of(
                        "stored_result_hash", failure.storedResultHash(),
                        "attempted_result_hash", failure.attemptedResultHash()
                ),
                null
        ));
    }
}
