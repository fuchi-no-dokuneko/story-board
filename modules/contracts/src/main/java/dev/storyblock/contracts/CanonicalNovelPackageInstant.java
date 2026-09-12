package dev.storyblock.contracts;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

final class CanonicalNovelPackageInstant {
    static Instant instant(Map<String, Object> value, String field, String path) {
        String text = CanonicalNovelPackageString.string(value, field, path);
        try {
            Instant instant = Instant.parse(text);
            if (!instant.toString().equals(text)) {
                throw new CanonicalPackageException(path + "." + field + " is not canonical UTC");
            }
            return instant;
        } catch (DateTimeParseException failure) {
            throw new CanonicalPackageException(path + "." + field + " is invalid", failure);
        }
    }
}
