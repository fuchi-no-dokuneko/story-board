package dev.storyblock.api.http;

import dev.storyblock.security.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

interface ApiAnalysisErrors extends ApiErrorContext {
  @ExceptionHandler(StyleAnalysisSnapshotConflictException.class)
  default ResponseEntity<Map<String, Object>> staleAnalysisSnapshot(
      StyleAnalysisSnapshotConflictException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerStaleAnalysisSnapshotAction.staleAnalysisSnapshot(context(), failure, request);
  }

  @ExceptionHandler(StyleAnalysisLeaseConflictException.class)
  default ResponseEntity<Map<String, Object>> analysisLeaseConflict(
      StyleAnalysisLeaseConflictException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerAnalysisLeaseConflictAction.analysisLeaseConflict(context(), failure, request);
  }

  @ExceptionHandler(StyleAnalysisResultConflictException.class)
  default ResponseEntity<Map<String, Object>> analysisResultConflict(
      StyleAnalysisResultConflictException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerAnalysisResultConflictAction.analysisResultConflict(context(), failure, request);
  }

  @ExceptionHandler(ExpiredStyleArtifactException.class)
  default ResponseEntity<Map<String, Object>> expiredStyleArtifact(
      ExpiredStyleArtifactException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
        HttpStatus.GONE,
        "ARTIFACT_EXPIRED",
        "Artifact expired",
        "artifact-expired",
        failure.getMessage()
    ));
  }
}
