package dev.storyblock.api.http;

import dev.storyblock.application.CommitRejectedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class ApiExceptionHandlerCommitRejectedAction {
    static ResponseEntity<Map<String, Object>> commitRejected(ApiExceptionHandler self, CommitRejectedException failure, HttpServletRequest request)  {
        return ApiExceptionHandlerResponse.response(request, new ApiFailureException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "DETERMINISTIC_VALIDATION_FAILED",
                "Deterministic validation failed",
                "deterministic-validation-failed",
                failure.getMessage(),
                Map.of(
                        "violations", failure.preview().violations(),
                        "warnings", failure.preview().warnings(),
                        "candidate_hash", failure.preview().candidateHash()
                ),
                null
        ));
    }
}
