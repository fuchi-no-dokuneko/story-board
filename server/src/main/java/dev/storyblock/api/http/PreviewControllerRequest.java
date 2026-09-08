package dev.storyblock.api.http;

import java.util.Map;
import java.util.Set;

final class PreviewControllerRequest {
    static Map<String, Object> request(byte[] bytes, String path) {
        Map<String, Object> request = StrictJsonRequest.parseObject(bytes, path);
        StrictJsonRequest.requireKeys(request, Set.of(
                "operation", "candidate_revision_id", "candidate_created_at"
        ), path);
        return request;
    }
}
