package dev.storyblock.api.http;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public final class ApiFailureException extends RuntimeException {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");
    private static final Pattern TYPE = Pattern.compile("[a-z][a-z0-9-]{1,63}");

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final String typeSlug;
    private final Map<String, Object> extensions;
    private final Integer retryAfterSeconds;

    public ApiFailureException(
            HttpStatus status,
            String code,
            String title,
            String typeSlug,
            String detail,
            Map<String, Object> extensions,
            Integer retryAfterSeconds
    ) {
        super(detail);
        this.status = Objects.requireNonNull(status, "status");
        if (!status.isError()) {
            throw new IllegalArgumentException("API failure status must be an error");
        }
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("API problem code is invalid");
        }
        if (typeSlug == null || !TYPE.matcher(typeSlug).matches()) {
            throw new IllegalArgumentException("API problem type is invalid");
        }
        this.code = code;
        this.title = Objects.requireNonNull(title, "title");
        this.typeSlug = typeSlug;
        this.extensions = Map.copyOf(extensions);
        if (retryAfterSeconds != null && retryAfterSeconds < 1) {
            throw new IllegalArgumentException("Retry-After must be positive");
        }
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ApiFailureException of(
            HttpStatus status,
            String code,
            String title,
            String typeSlug,
            String detail
    ) {
        return new ApiFailureException(
                status, code, title, typeSlug, detail, Map.of(), null
        );
    }

    public static ApiFailureException unavailable(String detail) {
        return new ApiFailureException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "DEPENDENCY_UNAVAILABLE",
                "Dependency unavailable",
                "dependency-unavailable",
                detail,
                Map.of(),
                1
        );
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String typeSlug() {
        return typeSlug;
    }

    public Map<String, Object> extensions() {
        return extensions;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
