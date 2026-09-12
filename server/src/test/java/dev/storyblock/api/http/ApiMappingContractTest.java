package dev.storyblock.api.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import dev.storyblock.contracts.CanonicalJson;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
class ApiMappingContractTest {
    @Autowired @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        String database = "target/mapping-" + UUID.randomUUID() + ".db";
        registry.add("storyblock.database.path", () -> database);
        registry.add("storyblock.security.pepper", () -> "mapping-test-pepper-at-least-thirty-two-bytes");
    }

    @Test @SuppressWarnings("unchecked")
    void everyDocumentedProgrammaticRouteIsStillRegistered() throws Exception {
        var manifest = (Map<String, Object>) CanonicalJson.parse(Files.readAllBytes(
                Path.of("../plugin/references/endpoints.json")), Map.class);
        Set<String> expected = new TreeSet<>();
        for (var endpoint : (List<Map<String, Object>>) manifest.get("endpoints")) {
            String path = endpoint.get("path").toString();
            if (path.startsWith("/v1/")) expected.add(endpoint.get("method") + " " + path);
        }
        Set<String> actual = new TreeSet<>();
        mappings.getHandlerMethods().forEach((mapping, method) -> {
            for (String path : mapping.getPatternValues()) {
                if (path.startsWith("/v1/")) mapping.getMethodsCondition().getMethods()
                        .forEach(verb -> actual.add(verb + " " + path));
            }
        });
        assertEquals(expected, actual);
    }
}
