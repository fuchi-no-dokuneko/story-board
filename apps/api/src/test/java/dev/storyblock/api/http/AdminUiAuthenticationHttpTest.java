package dev.storyblock.api.http;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUiAuthenticationHttpTest {
    private static final String OWNER_TOKEN =
            "admin-ui-owner-token-material-at-least-32-bytes";
    private static final Path DATABASE_PATH = Path.of(
            "target", "admin-ui-auth-" + UUID.randomUUID() + ".db"
    );

    @DynamicPropertySource
    static void configureApplication(DynamicPropertyRegistry registry) {
        registry.add("storyblock.database.path", DATABASE_PATH::toString);
        registry.add(
                "storyblock.security.pepper",
                () -> "admin-ui-auth-test-pepper-at-least-32-bytes"
        );
        registry.add("storyblock.security.owner-token", () -> OWNER_TOKEN);
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void ownerTokenUnlocksTheProtectedAdminLibrary() throws Exception {
        mvc.perform(get("/v1/admin/novels"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mvc.perform(get("/v1/admin/novels")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void servesTheSharedAuthenticationUiWithoutCredentials() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"operator-token\"")))
                .andExpect(content().string(containsString("src=\"/auth.js?")));

        mvc.perform(get("/auth.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("resolveSameOriginUrl")));
    }
}
