package dev.storyblock.api.http;

import dev.storyblock.storage.IdempotencyConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class ApiExceptionHandlerIdempotencyConflictAction {
    static ResponseEntity<Map<String, Object>> idempotencyConflict(ApiExceptionHandler self, IdempotencyConflictException failure, HttpServletRequest request)  {
        return ApiExceptionHandlerResponse.response(request, new ApiFailureException(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_CONFLICT",
                "Idempotency conflict",
                "idempotency-conflict",
                failure.getMessage(),
                Map.of(
                        "stored_operation_hash", failure.storedOperationHash(),
                        "attempted_operation_hash", failure.attemptedOperationHash()
                ),
                null
        ));
    }
}
