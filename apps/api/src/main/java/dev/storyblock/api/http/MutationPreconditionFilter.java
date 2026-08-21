package dev.storyblock.api.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public final class MutationPreconditionFilter extends OncePerRequestFilter {
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final long MAX_REQUEST_BYTES = 2L * 1024L * 1024L;

    private static final Set<String> MUTATION_METHODS = Set.of(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name()
    );
    private static final Set<String> WILDCARD_CREATION_ROUTES = Set.of(
            "/v1/novels",
            "/v1/imports",
            "/v1/style-profiles",
            "/v1/internal/jobs/claims"
    );
    private static final Pattern STRONG_ETAG = Pattern.compile(
            "\"sha256:[0-9a-f]{64}\""
    );

    private final ApiProblemWriter problemWriter;

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
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            reject(request, response, ApiFailureException.of(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency key required",
                    "idempotency-key-required",
                    "Mutation requests require Idempotency-Key."
            ));
            return;
        }
        if (idempotencyKey.length() > 200) {
            reject(request, response, ApiFailureException.of(
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
            reject(request, response, ApiFailureException.of(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "IF_MATCH_REQUIRED",
                    "If-Match required",
                    "if-match-required",
                    "Mutation requests require If-Match."
            ));
            return;
        }
        boolean wildcard = "*".equals(ifMatch);
        if ((!wildcard && !STRONG_ETAG.matcher(ifMatch).matches())
                || (wildcard && !allowsWildcardCreation(request))) {
            reject(request, response, ApiFailureException.of(
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
        if (contentLength > MAX_REQUEST_BYTES) {
            rejectTooLarge(request, response);
            return;
        }

        HttpServletRequest requestToUse = request;
        if (contentLength < 0) {
            byte[] body = request.getInputStream().readNBytes(
                    Math.toIntExact(MAX_REQUEST_BYTES + 1)
            );
            if (body.length > MAX_REQUEST_BYTES) {
                rejectTooLarge(request, response);
                return;
            }
            requestToUse = new BufferedBodyRequest(request, body);
        }
        filterChain.doFilter(requestToUse, response);
    }

    private static boolean allowsWildcardCreation(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && WILDCARD_CREATION_ROUTES.contains(request.getRequestURI());
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            ApiFailureException failure
    ) throws IOException {
        problemWriter.write(request, response, failure);
    }

    private void rejectTooLarge(
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

    private static final class BufferedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private BufferedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ByteArrayServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class ByteArrayServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream input;

        private ByteArrayServletInputStream(byte[] body) {
            this.input = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return input.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            return input.read(bytes, offset, length);
        }

        @Override
        public boolean isFinished() {
            return input.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            java.util.Objects.requireNonNull(listener, "listener");
            try {
                if (!isFinished()) {
                    listener.onDataAvailable();
                }
                if (isFinished()) {
                    listener.onAllDataRead();
                }
            } catch (IOException failure) {
                listener.onError(failure);
            }
        }
    }
}
