package dev.storyblock.api.http;

import dev.storyblock.storage.StaleHeadException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class ApiExceptionHandlerStaleHeadAction {
    static ResponseEntity<Map<String, Object>> staleHead(ApiExceptionHandler self, StaleHeadException failure, HttpServletRequest request)  {
        return ApiExceptionHandlerResponse.response(request, new ApiFailureException(
                HttpStatus.PRECONDITION_FAILED,
                "REVISION_CONFLICT",
                "Revision conflict",
                "revision-conflict",
                "If-Match does not match the current head.",
                Map.of(
                        "current_revision_id", failure.actual().revisionId().value(),
                        "current_etag", failure.actual().contentHash()
                ),
                null
        ));
    }
}
