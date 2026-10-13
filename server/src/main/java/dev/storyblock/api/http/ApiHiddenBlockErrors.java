package dev.storyblock.api.http;

import dev.storyblock.domain.HiddenBlockException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

interface ApiHiddenBlockErrors extends ApiErrorContext {
    @ExceptionHandler(HiddenBlockException.class)
    default ResponseEntity<Map<String, Object>> hiddenBlock(
            HiddenBlockException failure, HttpServletRequest request) {
        return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
                HttpStatus.UNPROCESSABLE_CONTENT, failure.code().name(),
                "Invalid hidden block", "hidden-block", failure.getMessage()));
    }
}
