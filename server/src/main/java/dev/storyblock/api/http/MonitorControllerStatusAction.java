package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MonitorRunStatus;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class MonitorControllerStatusAction {
    static ResponseEntity<Map<String, Object>> status(MonitorController self, String novelId, String runId, Authentication authentication)  {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        MonitorRunStatus status = self.monitors.getStatus(
                requestedNovel, new Ids.MonitorRunId(runId)
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, MonitorControllerQuote.quote(status.run().revisionHash()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(status.canonicalValue());
    }
}
