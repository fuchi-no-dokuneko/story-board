package dev.storyblock.api.http;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

final class StrictJsonRequestInstantFactory {
    static Instant instant(Map<String, Object> value, String field, String path)  {
        String text = StrictJsonRequest.string(value, field, path);
        try {
            Instant instant = Instant.parse(text);
            if (!instant.toString().equals(text)) {
                throw new IllegalArgumentException(
                        path + "." + field + " must use canonical UTC form"
                );
            }
            return instant;
        } catch (DateTimeParseException failure) {
            throw new IllegalArgumentException(
                    path + "." + field + " must be an ISO-8601 instant", failure
            );
        }
    }
}
