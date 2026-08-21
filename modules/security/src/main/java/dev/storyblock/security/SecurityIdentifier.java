package dev.storyblock.security;

import java.util.regex.Pattern;

final class SecurityIdentifier {
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");

    private SecurityIdentifier() {
    }

    static String require(String value, String field) {
        if (value == null || !SAFE.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a safe identifier");
        }
        return value;
    }

    static String optional(String value, String field) {
        return value == null ? null : require(value, field);
    }
}
