package dev.storyblock.api.http;

import dev.storyblock.security.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

interface ApiStyleErrors extends ApiErrorContext {
  @ExceptionHandler(StyleStatusPreconditionException.class)
  default ResponseEntity<Map<String, Object>> staleStyleStatus(
      StyleStatusPreconditionException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, new ApiFailureException(
        HttpStatus.PRECONDITION_FAILED,
        "STYLE_STATUS_CONFLICT",
        "Style status conflict",
        "style-status-conflict",
        failure.getMessage(),
        Map.of("current_etag", failure.currentHash()),
        null
    ));
  }

  @ExceptionHandler(StyleLifecycleConflictException.class)
  default ResponseEntity<Map<String, Object>> styleLifecycleConflict(
      StyleLifecycleConflictException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
        HttpStatus.CONFLICT,
        "STYLE_LIFECYCLE_CONFLICT",
        "Style lifecycle conflict",
        "style-lifecycle-conflict",
        failure.getMessage()
    ));
  }
}
