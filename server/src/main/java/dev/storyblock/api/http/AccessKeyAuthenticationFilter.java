package dev.storyblock.api.http;

import dev.storyblock.security.AccessKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

final class AccessKeyAuthenticationFilter extends OncePerRequestFilter {
  static final String BEARER_PREFIX = "Bearer ";

  final AccessKeyService accessKeys;
  final Clock clock;
  final byte[] ownerTokenHash;
  final ApiProblemWriter problemWriter;
  final StoryBlockTelemetry telemetry;
  final boolean trustedLan;

  AccessKeyAuthenticationFilter(
      AccessKeyService accessKeys,
      Clock clock,
      String ownerToken,
      ApiProblemWriter problemWriter,
      StoryBlockTelemetry telemetry,
      boolean trustedLan
  ) {
    this.accessKeys = java.util.Objects.requireNonNull(accessKeys, "accessKeys");
    this.clock = java.util.Objects.requireNonNull(clock, "clock");
    this.problemWriter = java.util.Objects.requireNonNull(problemWriter, "problemWriter");
    this.telemetry = java.util.Objects.requireNonNull(telemetry, "telemetry");
    this.trustedLan = trustedLan;
    this.ownerTokenHash = ownerToken == null || ownerToken.isBlank()
        ? null : AccessKeyAuthenticationFilterSha256.sha256(ownerToken);
    if (ownerTokenHash != null && ownerToken.length() < 32) {
      throw new IllegalArgumentException(
          "storyblock.security.owner-token must contain at least 32 characters"
      );
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return "/v1/openapi.yaml".equals(path)
        || path.equals("/actuator/health");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    AccessKeyAuthenticationFilterDoFilterInternalAction.doFilterInternal(this, request, response, filterChain);
  }

  boolean isOwnerToken(String token) {
    return ownerTokenHash != null && MessageDigest.isEqual(
        ownerTokenHash, AccessKeyAuthenticationFilterSha256.sha256(token)
    );
  }

  void reject(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    telemetry.recordAuthDenied("invalid");
    problemWriter.write(request, response, ApiFailureException.of(
        HttpStatus.UNAUTHORIZED,
        "INVALID_BEARER_CREDENTIAL",
        "Invalid bearer credential",
        "invalid-bearer-credential",
        "The bearer credential is invalid, expired, or revoked."
    ));
  }

}
