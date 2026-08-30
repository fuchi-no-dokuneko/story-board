package dev.storyblock.api.http;

import dev.storyblock.application.CommitRejectedException;
import dev.storyblock.application.ImagePayloadTooLargeException;
import dev.storyblock.security.AccessAuthenticationException;
import dev.storyblock.security.AccessKeyRequestConflictException;
import dev.storyblock.security.CrossNovelAccessException;
import dev.storyblock.security.MissingAccessKeyException;
import dev.storyblock.security.SecretAlreadyIssuedException;
import dev.storyblock.monitor.MissingMonitorRunException;
import dev.storyblock.style.MissingStyleProfileException;
import dev.storyblock.style.MissingStyleProfileVersionException;
import dev.storyblock.style.MissingStyleAnalysisException;
import dev.storyblock.style.MissingStyleAnalysisJobException;
import dev.storyblock.style.ExpiredStyleArtifactException;
import dev.storyblock.style.StyleAnalysisLeaseConflictException;
import dev.storyblock.style.StyleAnalysisResultConflictException;
import dev.storyblock.style.StyleAnalysisSnapshotConflictException;
import dev.storyblock.style.StyleLifecycleConflictException;
import dev.storyblock.style.StyleStatusPreconditionException;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.MissingArtifactException;
import dev.storyblock.storage.MissingExportJobException;
import dev.storyblock.storage.MissingNovelException;
import dev.storyblock.storage.MissingRevisionException;
import dev.storyblock.storage.NovelConflictException;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StorageException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public final class ApiExceptionHandler {
    private final boolean hideCrossNovel;

    public ApiExceptionHandler(
            @Value("${storyblock.security.hide-cross-novel:true}") boolean hideCrossNovel
    ) {
        this.hideCrossNovel = hideCrossNovel;
    }

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
            MissingArtifactException.class,
            MissingAccessKeyException.class,
            MissingMonitorRunException.class,
            MissingStyleProfileException.class,
            MissingStyleProfileVersionException.class,
            MissingStyleAnalysisException.class,
            MissingStyleAnalysisJobException.class
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

    @ExceptionHandler(StyleAnalysisSnapshotConflictException.class)
    ResponseEntity<Map<String, Object>> staleAnalysisSnapshot(
            StyleAnalysisSnapshotConflictException failure,
            HttpServletRequest request
    ) {
        return response(request, new ApiFailureException(
                HttpStatus.PRECONDITION_FAILED,
                "ANALYSIS_SNAPSHOT_CONFLICT",
                "Analysis snapshot conflict",
                "analysis-snapshot-conflict",
                failure.getMessage(),
                Map.of("current_etag", failure.currentRevisionHash()),
                null
        ));
    }

    @ExceptionHandler(StyleAnalysisLeaseConflictException.class)
    ResponseEntity<Map<String, Object>> analysisLeaseConflict(
            StyleAnalysisLeaseConflictException failure,
            HttpServletRequest request
    ) {
        return response(request, new ApiFailureException(
                HttpStatus.PRECONDITION_FAILED,
                "ANALYSIS_LEASE_CONFLICT",
                "Analysis lease conflict",
                "analysis-lease-conflict",
                failure.getMessage(),
                Map.of("current_etag", failure.currentStatusHash()),
                null
        ));
    }

    @ExceptionHandler(StyleAnalysisResultConflictException.class)
    ResponseEntity<Map<String, Object>> analysisResultConflict(
            StyleAnalysisResultConflictException failure,
            HttpServletRequest request
    ) {
        return response(request, new ApiFailureException(
                HttpStatus.CONFLICT,
                "ANALYSIS_RESULT_CONFLICT",
                "Analysis result conflict",
                "analysis-result-conflict",
                failure.getMessage(),
                Map.of(
                        "stored_result_hash", failure.storedResultHash(),
                        "attempted_result_hash", failure.attemptedResultHash()
                ),
                null
        ));
    }

    @ExceptionHandler(ExpiredStyleArtifactException.class)
    ResponseEntity<Map<String, Object>> expiredStyleArtifact(
            ExpiredStyleArtifactException failure,
            HttpServletRequest request
    ) {
        return response(request, ApiFailureException.of(
                HttpStatus.GONE,
                "ARTIFACT_EXPIRED",
                "Artifact expired",
                "artifact-expired",
                failure.getMessage()
        ));
    }

    @ExceptionHandler(StyleStatusPreconditionException.class)
    ResponseEntity<Map<String, Object>> staleStyleStatus(
            StyleStatusPreconditionException failure,
            HttpServletRequest request
    ) {
        return response(request, new ApiFailureException(
                HttpStatus.PRECONDITION_FAILED,
                "STYLE_STATUS_CONFLICT",
                "Style status conflict",
                "style-status-conflict",
                failure.getMessage(),
                Map.of("current_etag", failure.currentHash()),
                null
        ));
    }

    @ExceptionHandler(StyleLifecycleConflictException.class)
    ResponseEntity<Map<String, Object>> styleLifecycleConflict(
            StyleLifecycleConflictException failure,
            HttpServletRequest request
    ) {
        return response(request, ApiFailureException.of(
                HttpStatus.CONFLICT,
                "STYLE_LIFECYCLE_CONFLICT",
                "Style lifecycle conflict",
                "style-lifecycle-conflict",
                failure.getMessage()
        ));
    }

    @ExceptionHandler(CrossNovelAccessException.class)
    ResponseEntity<Map<String, Object>> crossNovel(
            CrossNovelAccessException ignored,
            HttpServletRequest request
    ) {
        ApiFailureException failure = hideCrossNovel
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
        return response(request, failure);
    }

    @ExceptionHandler(AccessAuthenticationException.class)
    ResponseEntity<Map<String, Object>> invalidCredential(
            AccessAuthenticationException ignored,
            HttpServletRequest request
    ) {
        return response(request, ApiFailureException.of(
                HttpStatus.UNAUTHORIZED,
                "INVALID_BEARER_CREDENTIAL",
                "Invalid bearer credential",
                "invalid-bearer-credential",
                "The bearer credential is invalid, expired, or revoked."
        ));
    }

    @ExceptionHandler({
            SecretAlreadyIssuedException.class,
            AccessKeyRequestConflictException.class
    })
    ResponseEntity<Map<String, Object>> accessKeyConflict(
            RuntimeException failure,
            HttpServletRequest request
    ) {
        return response(request, ApiFailureException.of(
                HttpStatus.CONFLICT,
                "ACCESS_KEY_CONFLICT",
                "Access key conflict",
                "access-key-conflict",
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
            MethodArgumentTypeMismatchException.class,
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

    @ExceptionHandler(ImagePayloadTooLargeException.class)
    ResponseEntity<Map<String, Object>> imageTooLarge(
            ImagePayloadTooLargeException failure,
            HttpServletRequest request
    ) {
        return response(request, new ApiFailureException(
                HttpStatus.CONTENT_TOO_LARGE,
                "REQUEST_TOO_LARGE",
                "Request too large",
                "request-too-large",
                "The uploaded image exceeds the configured byte limit.",
                Map.of("limit_bytes", failure.limitBytes()),
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
