package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import java.util.Map;

final class MonitorControllerBlockId {
    static Ids.BlockId blockId(
            Map<String, Object> request,
            String field,
            String path
    ) {
        return new Ids.BlockId(StrictJsonRequest.string(request, field, path));
    }
}
