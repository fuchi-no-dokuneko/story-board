package dev.storyblock.api.http;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public final class ApiExceptionHandler implements ApiEditErrors, ApiAnalysisErrors,
    ApiStyleErrors, ApiAccessErrors, ApiRequestErrors, ApiStorageErrors, ApiMediaErrors {
  final boolean hideCrossNovel;
  public ApiExceptionHandler(@Value("${storyblock.security.hide-cross-novel:true}") boolean hideCrossNovel) {
    this.hideCrossNovel = hideCrossNovel;
  }
  public ApiExceptionHandler context() { return this; }
}
