package dev.storyblock.api.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class ApiExceptionHandlerResponse {
    static ResponseEntity<Map<String, Object>> response(
            HttpServletRequest request,
            ApiFailureException failure
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.set(ApiRequestMetadata.REQUEST_ID_HEADER, ApiRequestMetadata.requestId(request));
        if (failure.retryAfterSeconds() != null) {
            headers.set(
                    HttpHeaders.RETRY_AFTER,
                    Integer.toString(failure.retryAfterSeconds())
            );
        }
        return new ResponseEntity<>(
                ApiProblemFactory.create(request, failure),
                headers,
                failure.status()
        );
    }
}
