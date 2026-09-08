package dev.storyblock.api.http;

import dev.storyblock.application.ImagePayloadTooLargeException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class ApiExceptionHandlerImageTooLargeAction {
    static ResponseEntity<Map<String, Object>> imageTooLarge(ApiExceptionHandler self, ImagePayloadTooLargeException failure, HttpServletRequest request)  {
        return ApiExceptionHandlerResponse.response(request, new ApiFailureException(
                HttpStatus.CONTENT_TOO_LARGE,
                "REQUEST_TOO_LARGE",
                "Request too large",
                "request-too-large",
                "The uploaded image exceeds the configured byte limit.",
                Map.of("limit_bytes", failure.limitBytes()),
                null
        ));
    }
}
