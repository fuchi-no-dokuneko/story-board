package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalJson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public final class ApiProblemWriter {
    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ApiFailureException failure
    ) throws IOException {
        response.setStatus(failure.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        if (failure.retryAfterSeconds() != null) {
            response.setHeader(
                    HttpHeaders.RETRY_AFTER,
                    Integer.toString(failure.retryAfterSeconds())
            );
        }
        response.getOutputStream().write(CanonicalJson.bytes(
                ApiProblemFactory.create(request, failure)
        ));
    }
}
