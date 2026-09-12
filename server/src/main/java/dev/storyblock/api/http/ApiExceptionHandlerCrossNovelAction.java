package dev.storyblock.api.http;

import dev.storyblock.security.CrossNovelAccessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class ApiExceptionHandlerCrossNovelAction {
    static ResponseEntity<Map<String, Object>> crossNovel(ApiExceptionHandler self, CrossNovelAccessException ignored, HttpServletRequest request)  {
        ApiFailureException failure = self.hideCrossNovel
                ? ApiFailureException.of(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "Resource not found",
                        "resource-not-found",
                        "The requested resource does not exist."
                )
                : ApiFailureException.of(
                        HttpStatus.FORBIDDEN,
                        "NOVEL_ACCESS_DENIED",
                        "Novel access denied",
                        "novel-access-denied",
                        "The credential cannot access the requested novel."
                );
        return ApiExceptionHandlerResponse.response(request, failure);
    }
}
