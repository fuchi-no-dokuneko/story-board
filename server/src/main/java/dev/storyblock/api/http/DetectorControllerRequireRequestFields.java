package dev.storyblock.api.http;

import java.util.Map;
import java.util.Set;
import static dev.storyblock.api.http.DetectorController.REQUEST_FIELDS;

final class DetectorControllerRequireRequestFields {
    static void requireRequestFields(Map<String, Object> request) {
        for (String required : Set.of("revision_id", "revision_hash")) {
            if (!request.containsKey(required)) {
                throw new IllegalArgumentException(
                        "detector request is missing " + required
                );
            }
        }
        for (String field : request.keySet()) {
            if (!REQUEST_FIELDS.contains(field)) {
                throw new IllegalArgumentException(
                        "detector request contains unknown field " + field
                );
            }
        }
    }
}
