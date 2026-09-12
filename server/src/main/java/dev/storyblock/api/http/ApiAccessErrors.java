package dev.storyblock.api.http;

import dev.storyblock.security.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

interface ApiAccessErrors extends ApiErrorContext {
  @ExceptionHandler(CrossNovelAccessException.class)
  default ResponseEntity<Map<String, Object>> crossNovel(
      CrossNovelAccessException ignored,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerCrossNovelAction.crossNovel(context(), ignored, request);
  }

  @ExceptionHandler(AccessAuthenticationException.class)
  default ResponseEntity<Map<String, Object>> invalidCredential(
      AccessAuthenticationException ignored,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
        HttpStatus.UNAUTHORIZED,
        "INVALID_BEARER_CREDENTIAL",
        "Invalid bearer credential",
        "invalid-bearer-credential",
        "The bearer credential is invalid, expired, or revoked."
    ));
  }

  @ExceptionHandler({
      SecretAlreadyIssuedException.class,
      AccessKeyRequestConflictException.class
  })
  default ResponseEntity<Map<String, Object>> accessKeyConflict(
      RuntimeException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
        HttpStatus.CONFLICT,
        "ACCESS_KEY_CONFLICT",
        "Access key conflict",
        "access-key-conflict",
        failure.getMessage()
    ));
  }
}
