package dev.storyblock.api.http;

import dev.storyblock.application.PdfRenderService;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.PdfRenderResult;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/novels/{novelId}/pdf-renders")
public final class PdfRenderController {
    private static final Set<String> REQUEST_FIELDS = Set.of("revision_id");

    private final PdfRenderService renders;

    public PdfRenderController(PdfRenderService renders) {
        this.renders = java.util.Objects.requireNonNull(renders, "renders");
    }

    @PostMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> render(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            Authentication authentication
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "PDF render request"
        );
        StrictJsonRequest.requireKeys(request, REQUEST_FIELDS, "PDF render request");
        Ids.RevisionId revisionId = new Ids.RevisionId(StrictJsonRequest.string(
                request, "revision_id", "PDF render request"
        ));
        PdfRenderResult result = renders.render(
                requestedNovel,
                revisionId,
                StrictJsonRequest.unquoteEtag(ifMatch)
        );
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(result.content().length)
                .header(HttpHeaders.ETAG, ifMatch)
                .header("X-PDF-Renderer-Version", result.rendererVersion())
                .header("X-PDF-Page-Count", Integer.toString(result.pageCount()))
                .header("X-PDF-Image-Count", Integer.toString(result.imageCount()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(requestedNovel.value() + ".pdf")
                                .build()
                                .toString()
                )
                .body(result.content());
    }
}
