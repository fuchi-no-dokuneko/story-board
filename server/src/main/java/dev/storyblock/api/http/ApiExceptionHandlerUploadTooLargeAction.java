package dev.storyblock.api.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

final class ApiExceptionHandlerUploadTooLargeAction {
    static ResponseEntity<Map<String, Object>> uploadTooLarge(ApiExceptionHandler self, MaxUploadSizeExceededException ignored, HttpServletRequest request)  {
        return ApiExceptionHandlerResponse.response(request, new ApiFailureException(
                HttpStatus.CONTENT_TOO_LARGE,
                "REQUEST_TOO_LARGE",
                "Request too large",
                "request-too-large",
                "The uploaded artifact exceeds the configured byte limit.",
                Map.of("limit_bytes", MutationPreconditionFilter.MAX_REQUEST_BYTES),
                null
        ));
    }
}
