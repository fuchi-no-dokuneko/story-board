package dev.storyblock.api.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public final class MutationPreconditionFilter extends OncePerRequestFilter {
  public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
  public static final long MAX_REQUEST_BYTES = 2L * 1024L * 1024L;

  static final Set<String> MUTATION_METHODS = Set.of(
      HttpMethod.POST.name(),
      HttpMethod.PUT.name(),
      HttpMethod.PATCH.name(),
      HttpMethod.DELETE.name()
  );
  static final Set<String> WILDCARD_CREATION_ROUTES = Set.of(
      "/v1/novels",
      "/v1/agent/novels",
      "/v1/imports",
      "/v1/style-profiles",
      "/v1/internal/jobs/claims"
  );
  static final Pattern STRONG_ETAG = Pattern.compile(
      "\"sha256:[0-9a-f]{64}\""
  );

  final ApiProblemWriter problemWriter;

  public MutationPreconditionFilter(ApiProblemWriter problemWriter) {
    this.problemWriter = java.util.Objects.requireNonNull(problemWriter, "problemWriter");
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/v1/")
        || !MUTATION_METHODS.contains(request.getMethod());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    MutationPreconditions.doFilterInternal(this, request, response, filterChain);
  }

  void reject(
      HttpServletRequest request,
      HttpServletResponse response,
      ApiFailureException failure
  ) throws IOException {
    problemWriter.write(request, response, failure);
  }

  void rejectTooLarge(
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    reject(request, response, new ApiFailureException(
        HttpStatus.CONTENT_TOO_LARGE,
        "REQUEST_TOO_LARGE",
        "Request too large",
        "request-too-large",
        "Request body exceeds the configured byte limit.",
        java.util.Map.of("limit_bytes", MAX_REQUEST_BYTES),
        null
    ));
  }

}
