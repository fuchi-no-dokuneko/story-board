package dev.storyblock.api.http;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import dev.storyblock.application.CommitRejectedException;
import dev.storyblock.application.ImagePayloadTooLargeException;
import dev.storyblock.security.*;
import dev.storyblock.monitor.MissingMonitorRunException;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
class ExceptionAdviceContractTest {
  @Test
  void retainsEveryExistingExceptionMapping() {
    var resolver = new ExceptionHandlerMethodResolver(ApiExceptionHandler.class);
    assertEquals("apiFailure", resolver.resolveMethodByExceptionType(ApiFailureException.class).getName());
    assertEquals("staleHead", resolver.resolveMethodByExceptionType(StaleHeadException.class).getName());
    assertEquals("idempotencyConflict", resolver.resolveMethodByExceptionType(IdempotencyConflictException.class).getName());
    assertEquals("commitRejected", resolver.resolveMethodByExceptionType(CommitRejectedException.class).getName());
    assertEquals("resourceConflict", resolver.resolveMethodByExceptionType(NovelConflictException.class).getName());
    assertEquals("staleAnalysisSnapshot", resolver.resolveMethodByExceptionType(StyleAnalysisSnapshotConflictException.class).getName());
    assertEquals("analysisLeaseConflict", resolver.resolveMethodByExceptionType(StyleAnalysisLeaseConflictException.class).getName());
    assertEquals("analysisResultConflict", resolver.resolveMethodByExceptionType(StyleAnalysisResultConflictException.class).getName());
    assertEquals("expiredStyleArtifact", resolver.resolveMethodByExceptionType(ExpiredStyleArtifactException.class).getName());
    assertEquals("staleStyleStatus", resolver.resolveMethodByExceptionType(StyleStatusPreconditionException.class).getName());
    assertEquals("styleLifecycleConflict", resolver.resolveMethodByExceptionType(StyleLifecycleConflictException.class).getName());
    assertEquals("crossNovel", resolver.resolveMethodByExceptionType(CrossNovelAccessException.class).getName());
    assertEquals("invalidCredential", resolver.resolveMethodByExceptionType(AccessAuthenticationException.class).getName());
    assertEquals("accessKeyConflict", resolver.resolveMethodByExceptionType(SecretAlreadyIssuedException.class).getName());
    assertEquals("accessKeyConflict", resolver.resolveMethodByExceptionType(AccessKeyRequestConflictException.class).getName());
    assertEquals("unknownRoute", resolver.resolveMethodByExceptionType(NoResourceFoundException.class).getName());
    assertEquals("malformedRequest", resolver.resolveMethodByExceptionType(HttpMessageNotReadableException.class).getName());
    assertEquals("malformedRequest", resolver.resolveMethodByExceptionType(MethodArgumentNotValidException.class).getName());
    assertEquals("malformedRequest", resolver.resolveMethodByExceptionType(MethodArgumentTypeMismatchException.class).getName());
    assertEquals("malformedRequest", resolver.resolveMethodByExceptionType(MissingServletRequestParameterException.class).getName());
    assertEquals("malformedRequest", resolver.resolveMethodByExceptionType(IllegalArgumentException.class).getName());
    assertEquals("uploadTooLarge", resolver.resolveMethodByExceptionType(MaxUploadSizeExceededException.class).getName());
    assertEquals("imageTooLarge", resolver.resolveMethodByExceptionType(ImagePayloadTooLargeException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingNovelException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingRevisionException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingExportJobException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingArtifactException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingAccessKeyException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingMonitorRunException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingStyleProfileException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingStyleProfileVersionException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingStyleAnalysisException.class).getName());
    assertEquals("missingResource", resolver.resolveMethodByExceptionType(MissingStyleAnalysisJobException.class).getName());
    assertEquals("storageUnavailable", resolver.resolveMethodByExceptionType(StorageException.class).getName());
  }
}
