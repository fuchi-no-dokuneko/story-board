package dev.storyblock.api.http;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.OrderKey;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
class ApiHttpContractTest {
    private static final String VALID_ETAG = "\"sha256:"
            + "0".repeat(64)
            + "\"";
    private static final Path DATABASE_PATH = Path.of(
            "target", "api-http-contract-" + UUID.randomUUID() + ".db"
    );

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("storyblock.database.path", DATABASE_PATH::toString);
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void servesOpenApiWithoutAuthentication() throws Exception {
        mvc.perform(get("/v1/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/yaml"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-cache")))
                .andExpect(content().string(containsString("openapi: 3.1.0")));
    }

    @Test
    void rejectsMissingAuthenticationWithProblemContract() throws Exception {
        mvc.perform(get("/v1/novels/nov_test")
                        .header(ApiRequestMetadata.REQUEST_ID_HEADER, "request-test-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(
                        ApiRequestMetadata.REQUEST_ID_HEADER,
                        "request-test-401"
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.request_id").value("request-test-401"));
    }

    @Test
    void rejectsAnAuthenticatedPrincipalWithoutTheRouteScope() throws Exception {
        mvc.perform(get("/v1/novels/nov_test")
                        .with(userWithScope("style:admin")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("SCOPE_REQUIRED"));
    }

    @Test
    void authorizedReadReachesTheDocumentedUnavailableScaffold() throws Exception {
        mvc.perform(get("/v1/novels/nov_test")
                        .with(userWithScope("novel:read")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }

    @Test
    void everyMutationRequiresAnIdempotencyKeyFirst() throws Exception {
        mvc.perform(post("/v1/novels")
                        .with(userWithScope("novel:admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.status").value(428))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void everyMutationRequiresIfMatchAfterIdempotency() throws Exception {
        mvc.perform(post("/v1/novels")
                        .with(userWithScope("novel:admin"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.status").value(428))
                .andExpect(jsonPath("$.code").value("IF_MATCH_REQUIRED"));
    }

    @Test
    void rejectsWeakOrMalformedEtags() throws Exception {
        mvc.perform(post("/v1/novels")
                        .with(userWithScope("novel:admin"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "request-1")
                        .header(HttpHeaders.IF_MATCH, "W/" + VALID_ETAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_IF_MATCH"));
    }

    @Test
    void rejectsWildcardEtagsOutsideCollectionCreation() throws Exception {
        mvc.perform(post("/v1/novels/nov_test/commits")
                        .with(userWithScope("novel:commit"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "request-1")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_IF_MATCH"));
    }

    @Test
    void rejectsOversizedIdempotencyKeys() throws Exception {
        mvc.perform(post("/v1/novels")
                        .with(userWithScope("novel:admin"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "x".repeat(201))
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void rejectsDeclaredBodiesOverTheConfiguredLimit() throws Exception {
        byte[] body = new byte[(int) MutationPreconditionFilter.MAX_REQUEST_BYTES + 1];
        mvc.perform(post("/v1/novels")
                        .with(userWithScope("novel:admin"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "request-1")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.code").value("REQUEST_TOO_LARGE"))
                .andExpect(jsonPath("$.limit_bytes")
                        .value(MutationPreconditionFilter.MAX_REQUEST_BYTES));
    }

    @Test
    void reportsMalformedJsonAfterPreconditionsPass() throws Exception {
        mvc.perform(post("/v1/novels")
                        .with(userWithScope("novel:admin"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "request-1")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void validMutationContractReachesOwningServiceBoundary() throws Exception {
        mvc.perform(post("/v1/novels")
                        .with(userWithScope("novel:admin"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "request-1")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }

    @Test
    void workerContractUsesItsDedicatedScope() throws Exception {
        mvc.perform(post("/v1/internal/jobs/claims")
                        .with(userWithScope("worker:execute"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "claim-1")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }

    @Test
    void importsAndExportsCanonicalContentThroughDurableHttpResources() throws Exception {
        CanonicalRevision revision = genesis();
        String novelId = stringField(revision.canonicalContent(), "novel_id");
        String revisionId = stringField(revision.canonicalContent(), "revision_id");
        byte[] importBody = CanonicalJson.bytes(Map.of(
                "format", CanonicalExportFormat.REVISION.canonicalName(),
                "document", revision.envelope()
        ));

        MockHttpServletRequestBuilder importRequest = post("/v1/imports")
                .with(userWithScope("novel:admin"))
                .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "http-import-1")
                .header(HttpHeaders.IF_MATCH, "*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(importBody);
        mvc.perform(importRequest)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/v1/novels/" + novelId))
                .andExpect(header().string(
                        HttpHeaders.ETAG, quotedEtag(revision.contentHash())
                ))
                .andExpect(jsonPath("$.novel_id").value(novelId))
                .andExpect(jsonPath("$.head_revision_id").value(revisionId))
                .andExpect(jsonPath("$.head_sequence").value(0))
                .andExpect(jsonPath("$.head_hash").value(revision.contentHash()));

        mvc.perform(importRequest)
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ETAG, quotedEtag(revision.contentHash())
                ));

        byte[] exportBody = CanonicalJson.bytes(Map.of(
                "revision_id", revisionId,
                "format", CanonicalExportFormat.PACKAGE.canonicalName()
        ));
        MvcResult acceptedResult = mvc.perform(post("/v1/novels/{novelId}/exports", novelId)
                        .with(userWithScope("novel:read"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "http-export-1")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportBody))
                .andExpect(status().isAccepted())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value("queued"))
                .andReturn();
        Map<String, Object> accepted = object(acceptedResult);
        String jobId = stringField(accepted, "job_id");

        MvcResult jobResult = mvc.perform(get("/v1/jobs/{jobId}", jobId)
                        .with(userWithScope("novel:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.novel_id").value(novelId))
                .andExpect(jsonPath("$.revision_id").value(revisionId))
                .andExpect(jsonPath("$.status").value("succeeded"))
                .andExpect(jsonPath("$.attempt").value(1))
                .andReturn();
        Map<String, Object> job = object(jobResult);
        String artifactId = stringField(job, "result_artifact_id");
        assertEquals("/v1/artifacts/" + artifactId, stringField(job, "result_uri"));

        MvcResult artifactResult = mvc.perform(get("/v1/artifacts/{artifactId}", artifactId)
                        .with(userWithScope("novel:read")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.storyblock.package+json"
                ))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(
                        CanonicalTransferController.ARTIFACT_CODEC_HEADER, "identity"
                ))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")
                ))
                .andReturn();
        byte[] artifactBytes = artifactResult.getResponse().getContentAsByteArray();
        CanonicalNovelPackage exported = CanonicalNovelPackage.parse(artifactBytes);
        assertEquals(revision.contentHash(), exported.manifest().headHash());
        assertEquals(
                quotedEtag(CanonicalJson.hashBytes(artifactBytes)),
                artifactResult.getResponse().getHeader(HttpHeaders.ETAG)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requiredRoutes")
    void everyScaffoldRouteIsMappedAndUsesItsDocumentedScope(
            String label,
            String method,
            String path,
            String scope,
            boolean wildcardAllowed
    ) throws Exception {
        MockHttpServletRequestBuilder request = switch (method) {
            case "GET" -> get(path);
            case "POST" -> post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}");
            case "DELETE" -> delete(path);
            default -> throw new AssertionError("unsupported test method " + method);
        };
        request.with(userWithScope(scope));
        if (!"GET".equals(method)) {
            request.header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "route-test-1");
            request.header(HttpHeaders.IF_MATCH, wildcardAllowed ? "*" : VALID_ETAG);
        }

        mvc.perform(request)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }

    private static Stream<Arguments> requiredRoutes() {
        return Stream.of(
                route("create novel", "POST", "/v1/novels", "novel:admin", true),
                route("read novel", "GET", "/v1/novels/nov_test", "novel:read", false),
                route("read revision", "GET", "/v1/novels/nov_test/revisions/rev_test", "novel:read", false),
                route("render", "POST", "/v1/novels/nov_test/renders", "novel:read", false),
                route("edit preview", "POST", "/v1/novels/nov_test/edit-previews", "novel:propose", false),
                route("commit", "POST", "/v1/novels/nov_test/commits", "novel:commit", false),
                route("undo preview", "POST", "/v1/novels/nov_test/undo-previews", "novel:propose", false),
                route("detector", "POST", "/v1/novels/nov_test/detector-runs", "novel:analyze", false),
                route("style analysis", "POST", "/v1/novels/nov_test/style-analyses", "style:analyze", false),
                route("read analysis", "GET", "/v1/style-analyses/analysis_test", "novel:read", false),
                route("rewrite", "POST", "/v1/rewrite-proposals", "rewrite:propose", false),
                route("read rewrite", "GET", "/v1/rewrite-proposals/proposal_test", "novel:read", false),
                route("create profile", "POST", "/v1/style-profiles", "style:admin", true),
                route("profile version", "POST", "/v1/style-profiles/profile_test/versions", "style:admin", false),
                route("create key", "POST", "/v1/novels/nov_test/access-keys", "novel:admin", false),
                route("revoke key", "DELETE", "/v1/access-keys/key_test", "novel:admin", false),
                route("claim job", "POST", "/v1/internal/jobs/claims", "worker:execute", true),
                route("submit worker result", "POST", "/v1/internal/jobs/job_test/results", "worker:execute", false)
        );
    }

    private static Arguments route(
            String label,
            String method,
            String path,
            String scope,
            boolean wildcardAllowed
    ) {
        return Arguments.of(label, method, path, scope, wildcardAllowed);
    }

    private static CanonicalRevision genesis() {
        String novelId = Ids.NovelId.create().value();
        String chapterId = Ids.ChapterId.create().value();
        Map<String, Object> block = Map.of(
                "id", Ids.BlockId.create().value(),
                "block_version_id", Ids.BlockVersionId.create().value(),
                "order_key", OrderKey.initial().value(),
                "text", "First sentence.",
                "meta", Map.of()
        );
        Map<String, Object> scene = Map.of(
                "id", Ids.SceneId.create().value(),
                "chapter_id", chapterId,
                "order_key", OrderKey.initial().value(),
                "title", "Opening",
                "transition_mode", "opening",
                "blocks", List.of(block)
        );
        Map<String, Object> chapter = Map.of(
                "id", chapterId,
                "order_key", OrderKey.initial().value(),
                "title", "Chapter",
                "scenes", List.of(scene)
        );
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schema_version", CanonicalRevision.SCHEMA_VERSION);
        document.put("novel_id", novelId);
        document.put("revision_id", Ids.RevisionId.create().value());
        document.put("parent_revision_id", null);
        document.put("chapters", List.of(chapter));
        document.put("created_at", Instant.parse("2026-08-21T12:00:00Z").toString());
        return CanonicalRevision.of(document);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(MvcResult result) {
        return CanonicalJson.parse(
                result.getResponse().getContentAsByteArray(), Map.class
        );
    }

    private static String stringField(Map<String, Object> object, String field) {
        return (String) object.get(field);
    }

    private static String quotedEtag(String hash) {
        return "\"" + hash + "\"";
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor
            userWithScope(String scope) {
        return user("contract-test").authorities(
                new SimpleGrantedAuthority("SCOPE_" + scope)
        );
    }
}
