package dev.storyblock.api.http;

import dev.storyblock.application.ImagePayloadTooLargeException;
import dev.storyblock.security.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

interface ApiRequestErrors extends ApiErrorContext {
  @ExceptionHandler(NoResourceFoundException.class)
  default ResponseEntity<Map<String, Object>> unknownRoute(
      NoResourceFoundException ignored,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
        HttpStatus.NOT_FOUND,
        "RESOURCE_NOT_FOUND",
        "Resource not found",
        "resource-not-found",
        "The requested resource does not exist."
    ));
  }

  @ExceptionHandler({
      HttpMessageNotReadableException.class,
      MethodArgumentNotValidException.class,
      MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class,
      IllegalArgumentException.class
  })
  default ResponseEntity<Map<String, Object>> malformedRequest(
      Exception ignored,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
        HttpStatus.BAD_REQUEST,
        "MALFORMED_REQUEST",
        "Malformed request",
        "malformed-request",
        "The request does not match the documented JSON or parameter contract."
    ));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  default ResponseEntity<Map<String, Object>> uploadTooLarge(
      MaxUploadSizeExceededException ignored,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerUploadTooLargeAction.uploadTooLarge(context(), ignored, request);
  }

  @ExceptionHandler(ImagePayloadTooLargeException.class)
  default ResponseEntity<Map<String, Object>> imageTooLarge(
      ImagePayloadTooLargeException failure,
      HttpServletRequest request
  ) {
    return ApiExceptionHandlerImageTooLargeAction.imageTooLarge(context(), failure, request);
  }
}
