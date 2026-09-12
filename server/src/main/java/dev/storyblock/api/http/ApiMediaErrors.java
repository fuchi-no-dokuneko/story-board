package dev.storyblock.api.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;

interface ApiMediaErrors {
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    default ResponseEntity<Map<String, Object>> unacceptableResponseType(
            HttpMediaTypeNotAcceptableException failure, HttpServletRequest request) {
        return ApiExceptionHandlerResponse.response(request, ApiFailureException.of(
                HttpStatus.NOT_ACCEPTABLE, "MALFORMED_REQUEST", "Malformed request",
                "malformed-request", "Accept must allow a documented response media type."));
    }
}
