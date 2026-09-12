package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.monitor.MonitorPacket;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class MonitorControllerPacketAction {
    static ResponseEntity<Map<String, Object>> packet(MonitorController self, String novelId, byte[] requestBytes, String ifMatch, Authentication authentication)  {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "monitor packet request"
        );
        StrictJsonRequest.requireKeys(
                request, MonitorController.PACKET_FIELDS, "monitor packet request"
        );
        String expectedHash = MonitorControllerMatchedRevisionHash.matchedRevisionHash(
                request, ifMatch, "monitor packet request"
        );
        MonitorPacket packet = self.monitors.packet(
                requestedNovel,
                MonitorControllerRevisionId.revisionId(request, "monitor packet request"),
                expectedHash,
                MonitorControllerBlockId.blockId(request, "target_block_id", "monitor packet request"),
                MonitorControllerExactInt.exactInt(request.get("neighbor_count"), "monitor packet request.neighbor_count")
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, MonitorControllerQuote.quote(packet.revisionHash()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(packet.canonicalValue());
    }
}
