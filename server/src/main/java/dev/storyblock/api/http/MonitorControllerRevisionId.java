package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import java.util.Map;

final class MonitorControllerRevisionId {
    static Ids.RevisionId revisionId(
            Map<String, Object> request,
            String path
    ) {
        return new Ids.RevisionId(StrictJsonRequest.string(
                request, "revision_id", path
        ));
    }
}
