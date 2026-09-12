package dev.storyblock.api.http;

import dev.storyblock.application.RenderService;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public final class RenderController {
    static final Set<String> REQUEST_FIELDS = Set.of(
            "revision_id", "from_block_id", "to_block_id"
    );

    private final RenderService renders;

    public RenderController(RenderService renders) {
        this.renders = java.util.Objects.requireNonNull(renders, "renders");
    }

    @PostMapping("/novels/{novelId}/renders")
    ResponseEntity<Map<String, Object>> render(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            Authentication authentication
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "render request"
        );
        RenderControllerRequireRequestFields.requireRequestFields(request);

        RenderRange range = new RenderRange(
                RenderControllerOptionalBlockId.optionalBlockId(request.get("from_block_id"), "from_block_id"),
                RenderControllerOptionalBlockId.optionalBlockId(request.get("to_block_id"), "to_block_id")
        );
        RenderPacket packet = renders.render(
                requestedNovel,
                new Ids.RevisionId(StrictJsonRequest.string(
                        request, "revision_id", "render request"
                )),
                StrictJsonRequest.unquoteEtag(ifMatch),
                range
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, '"' + packet.revisionHash() + '"')
                .body(packet.canonicalValue());
    }

}
