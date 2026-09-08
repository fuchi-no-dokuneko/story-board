package dev.storyblock.rewrite.policy;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

final class RewritePolicyCanonicalInstantFactory {
    static Instant instant(Map<String, Object> value, String field, String path)  {
        String raw = RewritePolicyCanonical.string(value, field, path);
        try {
            Instant result = Instant.parse(raw);
            if (!result.toString().equals(raw)) {
                throw new IllegalArgumentException(path + "." + field + " is not canonical");
            }
            return result;
        } catch (DateTimeParseException failure) {
            throw new IllegalArgumentException(path + "." + field + " is invalid", failure);
        }
    }
}
