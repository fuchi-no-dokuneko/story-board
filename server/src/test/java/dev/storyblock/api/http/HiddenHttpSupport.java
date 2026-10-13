package dev.storyblock.api.http;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.*;
import java.nio.file.Path;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="storyblock.trusted-lan.enabled=true")
@AutoConfigureMockMvc
abstract class HiddenHttpSupport {
    static final Path DB = Path.of("target","hidden-http-"+UUID.randomUUID()+".db");
    @DynamicPropertySource static void config(DynamicPropertyRegistry registry) {
        registry.add("storyblock.database.path",DB::toString);
        registry.add("storyblock.security.pepper",() -> "hidden-http-test-pepper-at-least-32-characters");
        registry.add("storyblock.styles.registry",() -> Path.of("config/content-config/styles.yaml").toAbsolutePath().toString());
    }
    @Autowired MockMvc mvc;
    RevisionManifest create() throws Exception {
        var revision = HiddenHttpFixture.revision();
        mvc.perform(post("/v1/novels").header("Idempotency-Key",UUID.randomUUID().toString()).header("If-Match","*")
                .contentType("application/json").content(CanonicalJson.bytes(Map.of(
                        "initial_revision",NarrativeCanonicalMapper.toCanonical(revision).envelope()))))
                .andExpect(status().isCreated());
        return revision;
    }
    static String path(RevisionManifest revision) {return "/v1/novels/"+revision.novel().id().value();}
    static String hash(RevisionManifest revision) {return NarrativeCanonicalMapper.toCanonical(revision).contentHash();}
    ResultActions postRevision(RevisionManifest revision,String route,Map<String,Object> body) throws Exception {
        String key = body.get("operation") instanceof Map<?,?> operation
                ? (String)operation.get("idempotency_key") : UUID.randomUUID().toString();
        return mvc.perform(post(path(revision)+route).header("If-Match",'"'+hash(revision)+'"')
                .header("Idempotency-Key",key)
                .contentType("application/json").content(CanonicalJson.bytes(body)));
    }
    ResultActions slice(RevisionManifest revision,int start,int end) throws Exception {
        return mvc.perform(get(path(revision)+"/scenes/"+revision.novel().chapters().getFirst().scenes().getFirst().id().value()+"/blocks")
                .param("revision_id",revision.id().value()).param("start",""+start).param("end",""+end));
    }
}
