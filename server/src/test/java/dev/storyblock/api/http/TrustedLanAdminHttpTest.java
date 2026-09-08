package dev.storyblock.api.http;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.storyblock.contracts.CanonicalJson;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "storyblock.trusted-lan.enabled=true")
@AutoConfigureMockMvc
class TrustedLanAdminHttpTest {
    private static final String NOVEL_ID =
            "nov_018f0f5e-7b4a-7c00-8000-000000000111";
    private static final String TEXT = "春夏秋冬東西南北天地。";
    private static final Path DATABASE_PATH = Path.of(
            "target", "trusted-lan-admin-" + UUID.randomUUID() + ".db"
    );

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("storyblock.database.path", DATABASE_PATH::toString);
        registry.add(
                "storyblock.security.pepper",
                () -> "trusted-lan-test-pepper-material-32-bytes"
        );
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void registersListsAndReadsPersistedNovelWithoutCredentials() throws Exception {
        byte[] request = registration(10);

        mvc.perform(post("/v1/agent/novels")
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "trusted-e2e-1")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/admin/novels/" + NOVEL_ID))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.idempotent_replay").value(false))
                .andExpect(jsonPath("$.novel.novel_id").value(NOVEL_ID))
                .andExpect(jsonPath("$.novel.han_character_count").value(10))
                .andExpect(jsonPath("$.novel.zombie_count").value(1000))
                .andExpect(jsonPath("$.novel.tnt_cannon_count").value(1000))
                .andExpect(jsonPath("$.novel.main_characters", hasSize(5)));

        mvc.perform(post("/v1/agent/novels")
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "trusted-e2e-1")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotent_replay").value(true));

        mvc.perform(get("/v1/admin/novels").queryParam("q", "方塊村"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("方塊村防線"));

        mvc.perform(get("/v1/admin/novels").queryParam("q", "zh-hant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        mvc.perform(get("/v1/admin/novels").queryParam("q", "米婭"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        mvc.perform(get("/v1/admin/novels/{novelId}", NOVEL_ID))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.novel.han_character_count").value(10))
                .andExpect(jsonPath("$.revision.chapters[0].scenes[0].blocks[0].text")
                        .value(TEXT));
    }

    @Test
    void rejectsADeclaredHanCountThatDoesNotMatchTheManuscript() throws Exception {
        mvc.perform(post("/v1/agent/novels")
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "trusted-invalid-1")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(9)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void servesTheLibraryAsAReadOnlyFirstScreen() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Novel library")))
                .andExpect(content().string(containsString("Read only")))
                .andExpect(content().string(containsString("All novels")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("<option>POST</option>")
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("<option>DELETE</option>")
                )));
    }

    private static byte[] registration(int expectedHanCharacters) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("novel_id", NOVEL_ID);
        body.put("created_at", "2026-08-24T12:00:00Z");
        body.put("title", "方塊村防線");
        body.put("language", "zh-Hant");
        body.put("main_characters", List.of("阿青", "小石", "米婭", "老鐵", "雲舟"));
        body.put("zombie_count", 1000);
        body.put("tnt_cannon_count", 1000);
        body.put("expected_han_characters", expectedHanCharacters);
        body.put("chapters", List.of(Map.of("title", "第一章", "text", TEXT)));
        return CanonicalJson.bytes(body);
    }
}
