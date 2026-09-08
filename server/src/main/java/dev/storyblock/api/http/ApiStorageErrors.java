package dev.storyblock.api.http;

import dev.storyblock.security.*;
import dev.storyblock.monitor.MissingMonitorRunException;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

interface ApiStorageErrors extends ApiErrorContext {
  @ExceptionHandler({
      MissingNovelException.class,
      MissingRevisionException.class,
      MissingExportJobException.class,
      MissingArtifactException.class,
      MissingAccessKeyException.class,
      MissingMonitorRunException.class,
      MissingStyleProfileException.class,
      MissingStyleProfileVersionException.class,
      MissingStyleAnalysisException.class,
      MissingStyleAnalysisJobException.class
  })
  default ResponseEntity<Map<String, Object>> missingResource(
      RuntimeException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
        HttpStatus.NOT_FOUND,
        "RESOURCE_NOT_FOUND",
        "Resource not found",
        "resource-not-found",
        failure.getMessage()
    ));
  }

  @ExceptionHandler(StorageException.class)
  default ResponseEntity<Map<String, Object>> storageUnavailable(
      StorageException ignored,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, ApiFailureException.unavailable(
        "Canonical storage is temporarily unavailable."
    ));
  }
}
