package dev.storyblock.api.http;

import dev.storyblock.application.CommitRejectedException;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.MissingNovelException;
import dev.storyblock.storage.MissingRevisionException;
import dev.storyblock.storage.MissingArtifactException;
import dev.storyblock.storage.MissingExportJobException;
import dev.storyblock.storage.NovelConflictException;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StorageException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public final class ApiExceptionHandler {
    @ExceptionHandler(ApiFailureException.class)
    ResponseEntity<Map<String, Object>> apiFailure(
            ApiFailureException failure,
            HttpServletRequest request
    ) {
        return response(request, failure);
    }

    @ExceptionHandler(StaleHeadException.class)
    ResponseEntity<Map<String, Object>> staleHead(
            StaleHeadException failure,
            HttpServletRequest request
    ) {
        return response(request, new ApiFailureException(
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

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<Map<String, Object>> idempotencyConflict(
            IdempotencyConflictException failure,
            HttpServletRequest request
    ) {
        return response(request, new ApiFailureException(
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

    @ExceptionHandler(CommitRejectedException.class)
    ResponseEntity<Map<String, Object>> commitRejected(
            CommitRejectedException failure,
            HttpServletRequest request
    ) {
        return response(request, new ApiFailureException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "DETERMINISTIC_VALIDATION_FAILED",
                "Deterministic validation failed",
                "deterministic-validation-failed",
                failure.getMessage(),
                Map.of(
                        "violations", failure.preview().violations(),
                        "warnings", failure.preview().warnings(),
                        "candidate_hash", failure.preview().candidateHash()
                ),
                null
        ));
    }

    @ExceptionHandler({
            MissingNovelException.class,
            MissingRevisionException.class,
            MissingExportJobException.class,
            MissingArtifactException.class
    })
    ResponseEntity<Map<String, Object>> missingResource(
            RuntimeException failure,
            HttpServletRequest request
    ) {
        return response(request, ApiFailureException.of(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Resource not found",
                "resource-not-found",
                failure.getMessage()
        ));
    }

    @ExceptionHandler(NovelConflictException.class)
    ResponseEntity<Map<String, Object>> resourceConflict(
            NovelConflictException failure,
            HttpServletRequest request
    ) {
        return response(request, ApiFailureException.of(
                HttpStatus.CONFLICT,
                "RESOURCE_CONFLICT",
                "Resource conflict",
                "resource-conflict",
                failure.getMessage()
        ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> unknownRoute(
            NoResourceFoundException ignored,
            HttpServletRequest request
    ) {
        return response(request, ApiFailureException.of(
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
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    ResponseEntity<Map<String, Object>> malformedRequest(
            Exception ignored,
            HttpServletRequest request
    ) {
        return response(request, ApiFailureException.of(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Malformed request",
                "malformed-request",
                "The request does not match the documented JSON or parameter contract."
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> uploadTooLarge(
            MaxUploadSizeExceededException ignored,
            HttpServletRequest request
    ) {
        return response(request, new ApiFailureException(
                HttpStatus.CONTENT_TOO_LARGE,
                "REQUEST_TOO_LARGE",
                "Request too large",
                "request-too-large",
                "The uploaded artifact exceeds the configured byte limit.",
                Map.of("limit_bytes", MutationPreconditionFilter.MAX_REQUEST_BYTES),
                null
        ));
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<Map<String, Object>> storageUnavailable(
            StorageException ignored,
            HttpServletRequest request
    ) {
        return response(request, ApiFailureException.unavailable(
                "Canonical storage is temporarily unavailable."
        ));
    }

    private static ResponseEntity<Map<String, Object>> response(
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
