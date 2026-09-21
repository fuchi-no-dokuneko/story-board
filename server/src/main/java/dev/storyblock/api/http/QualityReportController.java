package dev.storyblock.api.http;

import dev.storyblock.api.quality.NovelQualityService;
import java.util.*;
import java.util.concurrent.Callable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public final class QualityReportController {
    private final NovelQualityService service;
    public QualityReportController(NovelQualityService service) { this.service = service; }
    @PostMapping("/v1/novels/{novelId}/quality-reports")
    Callable<ResponseEntity<Map<String,Object>>> report(@PathVariable String novelId,
            @RequestBody byte[] bytes, @RequestHeader("If-Match") String ifMatch) {
        var request = StrictJsonRequest.parseObject(bytes,"quality report");
        StyleAnalysisControllerRequireFields.requireFields(request,Set.of(),Set.of("revision_id","proper_names"),"quality report");
        String revision = request.containsKey("revision_id") ? StrictJsonRequest.string(request,"revision_id","quality report") : null;
        var names = request.containsKey("proper_names") ? Set.copyOf(StrictJsonRequest.uniqueStrings(request,"proper_names","quality report")) : Set.<String>of();
        String hash = StrictJsonRequest.unquoteEtag(ifMatch);
        return () -> ResponseEntity.ok().header("Cache-Control","no-store").body(service.analyze(novelId,revision,hash,names));
    }
}
