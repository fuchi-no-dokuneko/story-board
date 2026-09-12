package dev.storyblock.api.http;

import dev.storyblock.application.CommitRejectedException;
import dev.storyblock.security.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

interface ApiEditErrors extends ApiErrorContext {
  @ExceptionHandler(ApiFailureException.class)
  default ResponseEntity<Map<String, Object>> apiFailure(
      ApiFailureException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, failure);
  }

  @ExceptionHandler(StaleHeadException.class)
  default ResponseEntity<Map<String, Object>> staleHead(
      StaleHeadException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerStaleHeadAction.staleHead(context(), failure, request);
  }

  @ExceptionHandler(IdempotencyConflictException.class)
  default ResponseEntity<Map<String, Object>> idempotencyConflict(
      IdempotencyConflictException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerIdempotencyConflictAction.idempotencyConflict(context(), failure, request);
  }

  @ExceptionHandler(CommitRejectedException.class)
  default ResponseEntity<Map<String, Object>> commitRejected(
      CommitRejectedException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerCommitRejectedAction.commitRejected(context(), failure, request);
  }

  @ExceptionHandler(NovelConflictException.class)
  default ResponseEntity<Map<String, Object>> resourceConflict(
      NovelConflictException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
        HttpStatus.CONFLICT,
        "RESOURCE_CONFLICT",
        "Resource conflict",
        "resource-conflict",
        failure.getMessage()
    ));
  }
}
