package dev.storyblock.api.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

final class MutationPreconditions {
  static void doFilterInternal(MutationPreconditionFilter self, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String idempotencyKey = request.getHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY);
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      self.reject(request, response, ApiFailureException.of(
          HttpStatus.PRECONDITION_REQUIRED,
          "IDEMPOTENCY_KEY_REQUIRED",
          "Idempotency key required",
          "idempotency-key-required",
          "Mutation requests require Idempotency-Key."
      ));
      return;
    }
    if (idempotencyKey.length() > 200) {
      self.reject(request, response, ApiFailureException.of(
          HttpStatus.BAD_REQUEST,
          "INVALID_IDEMPOTENCY_KEY",
          "Invalid idempotency key",
          "invalid-idempotency-key",
          "Idempotency-Key must contain 1 to 200 characters."
      ));
      return;
    }

    String ifMatch = request.getHeader(HttpHeaders.IF_MATCH);
    if (ifMatch == null || ifMatch.isBlank()) {
      self.reject(request, response, ApiFailureException.of(
          HttpStatus.PRECONDITION_REQUIRED,
          "IF_MATCH_REQUIRED",
          "If-Match required",
          "if-match-required",
          "Mutation requests require If-Match."
      ));
      return;
    }
    boolean wildcard = "*".equals(ifMatch);
    if ((!wildcard && !MutationPreconditionFilter.STRONG_ETAG.matcher(ifMatch).matches())
        || (wildcard && !MutationPreconditionFilterAllowsWildcardCreation.allowsWildcardCreation(request))) {
      self.reject(request, response, ApiFailureException.of(
          HttpStatus.BAD_REQUEST,
          "INVALID_IF_MATCH",
          "Invalid If-Match",
          "invalid-if-match",
          "If-Match must be a single strong SHA-256 ETag; wildcard is limited "
              + "to collection creation."
      ));
      return;
    }

    long contentLength = request.getContentLengthLong();
    if (contentLength > MutationPreconditionFilter.MAX_REQUEST_BYTES) {
      self.rejectTooLarge(request, response);
      return;
    }

    HttpServletRequest requestToUse = request;
    if (contentLength < 0) {
      byte[] body = request.getInputStream().readNBytes(
          Math.toIntExact(MutationPreconditionFilter.MAX_REQUEST_BYTES + 1)
      );
      if (body.length > MutationPreconditionFilter.MAX_REQUEST_BYTES) {
        self.rejectTooLarge(request, response);
        return;
      }
      requestToUse = new BufferedMutationRequest(request, body);
    }
    filterChain.doFilter(requestToUse, response);
  }
}
