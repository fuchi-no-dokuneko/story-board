package dev.storyblock.api.http;

import java.util.Map;
import static dev.storyblock.api.http.RenderController.REQUEST_FIELDS;

final class RenderControllerRequireRequestFields {
    static void requireRequestFields(Map<String, Object> request) {
        if (!request.containsKey("revision_id")) {
            throw new IllegalArgumentException("render request is missing revision_id");
        }
        for (String field : request.keySet()) {
            if (!REQUEST_FIELDS.contains(field)) {
                throw new IllegalArgumentException(
                        "render request contains unknown field " + field
                );
            }
        }
    }
}
