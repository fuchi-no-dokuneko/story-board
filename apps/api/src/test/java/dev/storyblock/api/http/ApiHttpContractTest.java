package dev.storyblock.api.http;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditResult;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import dev.storyblock.validator.EvidenceSpans;
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
    private static final String OWNER_TOKEN =
            "test-owner-bootstrap-token-material-32-bytes";
    private static final String VALID_ETAG = "\"sha256:"
            + "0".repeat(64)
            + "\"";
    private static final Path DATABASE_PATH = Path.of(
            "target", "api-http-contract-" + UUID.randomUUID() + ".db"
    );

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("storyblock.database.path", DATABASE_PATH::toString);
        registry.add("storyblock.security.pepper", () -> "test-pepper-material-32-bytes-minimum");
        registry.add("storyblock.security.owner-token", () -> OWNER_TOKEN);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SqliteRevisionStore store;

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

    @Test
    void rendersStoredRevisionAsARepeatableCanonicalPacket() throws Exception {
        CanonicalRevision revision = genesis();
        String novelId = stringField(revision.canonicalContent(), "novel_id");
        String revisionId = stringField(revision.canonicalContent(), "revision_id");
        importAsOwner(revision, "render-import");
        byte[] body = CanonicalJson.bytes(Map.of("revision_id", revisionId));

        mvc.perform(post("/v1/novels/{novelId}/renders", novelId)
                        .with(userWithScope("style:admin"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "render-denied")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCOPE_REQUIRED"));

        MvcResult first = mvc.perform(post("/v1/novels/{novelId}/renders", novelId)
                        .with(userWithScope("novel:read"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "render-first")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ETAG, quotedEtag(revision.contentHash())
                ))
                .andExpect(jsonPath("$.novel_id").value(novelId))
                .andExpect(jsonPath("$.revision_id").value(revisionId))
                .andExpect(jsonPath("$.revision_hash").value(revision.contentHash()))
                .andExpect(jsonPath("$.rendered_text").value("First sentence."))
                .andExpect(jsonPath("$.offset_map[0].rendered_start").value(0))
                .andExpect(jsonPath("$.offset_map[0].rendered_end").value(15))
                .andExpect(jsonPath("$.resolved_meta[0].before.weather.mode")
                        .value("unknown"))
                .andExpect(jsonPath("$.scene_boundaries[0].state_out.weather.mode")
                        .value("unknown"))
                .andReturn();

        MvcResult second = mvc.perform(post("/v1/novels/{novelId}/renders", novelId)
                        .with(userWithScope("novel:read"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "render-second")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertArrayEquals(
                first.getResponse().getContentAsByteArray(),
                second.getResponse().getContentAsByteArray()
        );

        mvc.perform(post("/v1/novels/{novelId}/renders", novelId)
                        .with(userWithScope("novel:read"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "render-stale")
                        .header(HttpHeaders.IF_MATCH, VALID_ETAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("REVISION_CONFLICT"))
                .andExpect(jsonPath("$.current_revision_id").value(revisionId))
                .andExpect(jsonPath("$.current_etag").value(revision.contentHash()));
    }

    @Test
    void runsRevisionBoundDetectorWithStableFindingBytes() throws Exception {
        CanonicalRevision revision = detectorRevision();
        String novelId = stringField(revision.canonicalContent(), "novel_id");
        String revisionId = stringField(revision.canonicalContent(), "revision_id");
        importAsOwner(revision, "detector-import");
        byte[] body = CanonicalJson.bytes(Map.of(
                "revision_id", revisionId,
                "revision_hash", revision.contentHash()
        ));

        mvc.perform(post("/v1/novels/{novelId}/detector-runs", novelId)
                        .with(userWithScope("novel:read"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "detector-denied")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCOPE_REQUIRED"));

        MvcResult first = mvc.perform(post("/v1/novels/{novelId}/detector-runs", novelId)
                        .with(userWithScope("novel:analyze"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "detector-first")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ETAG, quotedEtag(revision.contentHash())
                ))
                .andExpect(jsonPath("$.revision_id").value(revisionId))
                .andExpect(jsonPath("$.revision_hash").value(revision.contentHash()))
                .andExpect(jsonPath("$.rule_version").value("detector-1.0.0"))
                .andExpect(jsonPath("$.findings[0].finding_id").isString())
                .andExpect(jsonPath("$.findings[0].code")
                        .value("LOCATION_CHANGED_WITHOUT_TRANSITION"))
                .andExpect(jsonPath("$.findings[0].severity").value("warning"))
                .andExpect(jsonPath("$.findings[0].affected_scene_ids.length()")
                        .value(2))
                .andExpect(jsonPath("$.findings[0].context_block_ids.length()")
                        .value(2))
                .andExpect(jsonPath("$.findings[0].evidence.kind")
                        .value("scene_boundary"))
                .andReturn();

        MvcResult second = mvc.perform(post("/v1/novels/{novelId}/detector-runs", novelId)
                        .with(userWithScope("novel:analyze"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "detector-second")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertArrayEquals(
                first.getResponse().getContentAsByteArray(),
                second.getResponse().getContentAsByteArray()
        );

        String staleHash = "sha256:" + "0".repeat(64);
        byte[] mismatchedBody = CanonicalJson.bytes(Map.of(
                "revision_id", revisionId,
                "revision_hash", staleHash
        ));
        mvc.perform(post("/v1/novels/{novelId}/detector-runs", novelId)
                        .with(userWithScope("novel:analyze"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "detector-mismatch")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mismatchedBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mvc.perform(post("/v1/novels/{novelId}/detector-runs", novelId)
                        .with(userWithScope("novel:analyze"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "detector-stale")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(staleHash))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mismatchedBody))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("REVISION_CONFLICT"))
                .andExpect(jsonPath("$.current_revision_id").value(revisionId))
                .andExpect(jsonPath("$.current_etag").value(revision.contentHash()));
    }

    @Test
    void monitorPacketsAndSubmissionsUseSeparateLeastPrivilegeScopes() throws Exception {
        CanonicalRevision revision = detectorRevision();
        String novelId = stringField(revision.canonicalContent(), "novel_id");
        String revisionId = stringField(revision.canonicalContent(), "revision_id");
        String blockId = firstBlockId(revision);
        importAsOwner(revision, "monitor-import");
        byte[] packetBody = CanonicalJson.bytes(Map.of(
                "revision_id", revisionId,
                "revision_hash", revision.contentHash(),
                "target_block_id", blockId,
                "neighbor_count", 1
        ));

        mvc.perform(post("/v1/novels/{novelId}/monitor-packets", novelId)
                        .with(userWithScope("novel:analyze"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "packet-denied")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(packetBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCOPE_REQUIRED"));

        mvc.perform(post("/v1/novels/{novelId}/monitor-packets", novelId)
                        .with(userWithScope("novel:read"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "packet-read")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(packetBody))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ETAG, quotedEtag(revision.contentHash())
                ))
                .andExpect(jsonPath("$.target_block_id").value(blockId))
                .andExpect(jsonPath("$.render_packet.blocks.length()").value(2))
                .andExpect(jsonPath("$.local_invariants.window_blocks.length()").value(2))
                .andExpect(jsonPath("$.allowed_tools[0]").value("submit_finding"))
                .andExpect(jsonPath("$.allowed_tools[1]")
                        .value("submit_proposed_operation"));

        String quote = "The";
        byte[] submissionBody = CanonicalJson.bytes(Map.of(
                "revision_id", revisionId,
                "revision_hash", revision.contentHash(),
                "target_block_id", blockId,
                "neighbor_count", 1,
                "rule_version", "monitor-rules-1.0.0",
                "affected_block_ids", List.of(blockId),
                "output", Map.of(
                        "kind", "finding",
                        "code", "LOCAL_NOTE",
                        "severity", "info",
                        "message", "Review this local passage.",
                        "evidence", List.of(Map.of(
                                "block_id", blockId,
                                "start_grapheme", 0,
                                "end_grapheme", 3,
                                "quote", quote,
                                "quote_hash", EvidenceSpans.quoteHash(quote)
                        ))
                )
        ));
        MockHttpServletRequestBuilder submit = post(
                "/v1/novels/{novelId}/monitor-runs", novelId
        )
                .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "monitor-submit")
                .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(submissionBody);

        mvc.perform(post("/v1/novels/{novelId}/monitor-runs", novelId)
                        .with(userWithScope("novel:read"))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "submit-denied")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCOPE_REQUIRED"));

        MvcResult created = mvc.perform(submit.with(userWithScope("monitor:submit")))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.output_kind").value("finding"))
                .andExpect(jsonPath("$.state").value("current"))
                .andExpect(jsonPath("$.stale_reasons.length()").value(0))
                .andExpect(jsonPath("$.rebase_allowed").value(false))
                .andExpect(jsonPath("$.idempotent_replay").value(false))
                .andReturn();
        String runId = stringField(object(created), "monitor_run_id");

        mvc.perform(submit.with(userWithScope("monitor:submit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitor_run_id").value(runId))
                .andExpect(jsonPath("$.idempotent_replay").value(true));

        mvc.perform(get("/v1/novels/{novelId}/monitor-runs/{runId}", novelId, runId)
                        .with(userWithScope("novel:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitor_run_id").value(runId))
                .andExpect(jsonPath("$.state").value("current"))
                .andExpect(jsonPath("$.rebase_allowed").value(false));
    }

    @Test
    void realBearerCredentialsEnforceScopesNovelBoundariesAndRevocation()
            throws Exception {
        CanonicalRevision first = genesis();
        CanonicalRevision second = genesis();
        String firstNovel = stringField(first.canonicalContent(), "novel_id");
        String secondNovel = stringField(second.canonicalContent(), "novel_id");
        String secondRevision = stringField(second.canonicalContent(), "revision_id");
        importAsOwner(first, "security-import-first");
        importAsOwner(second, "security-import-second");

        Map<String, Object> key = issueAsOwner(
                first,
                "security-issue-first",
                List.of(
                        "novel:read",
                        "novel:analyze",
                        "novel:admin",
                        "novel:commit",
                        "style:analyze",
                        "rewrite:propose"
                )
        );
        String bearer = stringField(key, "secret");
        String keyId = stringField(key, "key_id");

        byte[] escalationBody = CanonicalJson.bytes(Map.of(
                "actor_id", "escalated-worker",
                "scopes", List.of("worker:execute"),
                "expires_at", "2099-01-01T00:00:00Z"
        ));
        mvc.perform(post("/v1/novels/{novelId}/access-keys", firstNovel)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "scope-denied")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(first.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(escalationBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCOPE_DELEGATION_DENIED"));
        byte[] expiryEscalationBody = CanonicalJson.bytes(Map.of(
                "actor_id", "long-lived-reader",
                "scopes", List.of("novel:read"),
                "expires_at", "2100-01-01T00:00:00Z"
        ));
        mvc.perform(post("/v1/novels/{novelId}/access-keys", firstNovel)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "expiry-denied")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(first.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expiryEscalationBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXPIRY_DELEGATION_DENIED"));
        mvc.perform(post("/v1/style-profiles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "scope-route-denied")
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCOPE_REQUIRED"));

        Map<String, Object> ownExport = startExport(
                first, bearer, "security-export-first"
        );
        String ownJobId = stringField(ownExport, "job_id");
        mvc.perform(get("/v1/jobs/{jobId}", ownJobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.novel_id").value(firstNovel));

        mvc.perform(get("/v1/novels/{novelId}", secondNovel)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail")
                        .value("The requested resource does not exist."));

        mvc.perform(post("/v1/novels/{novelId}/style-analyses", secondNovel)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "cross-analysis")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(second.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        byte[] rewriteBody = CanonicalJson.bytes(Map.of(
                "novel_id", secondNovel,
                "revision_id", secondRevision,
                "revision_hash", second.contentHash(),
                "finding_ids", List.of("finding_test")
        ));
        mvc.perform(post("/v1/rewrite-proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "cross-rewrite")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(second.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rewriteBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        String mismatchKey = "cross-body-commit";
        Map<String, Object> mismatchedOperation = Map.of(
                "operation_id", Ids.OperationId.create().value(),
                "idempotency_key", mismatchKey,
                "novel_id", secondNovel,
                "base_revision_id", secondRevision,
                "expected_head_hash", second.contentHash(),
                "type", "restore_revision_content",
                "payload", Map.of(
                        "restore_revision_id", secondRevision,
                        "expected_restore_hash", second.contentHash()
                )
        );
        byte[] mismatchBody = CanonicalJson.bytes(Map.of(
                "operation", mismatchedOperation,
                "candidate_revision_id", Ids.RevisionId.create().value(),
                "candidate_created_at", "2026-08-21T12:10:00Z"
        ));
        mvc.perform(post("/v1/novels/{novelId}/commits", firstNovel)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, mismatchKey)
                        .header(HttpHeaders.IF_MATCH, quotedEtag(first.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mismatchBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        Map<String, Object> otherExport = startExport(
                second, OWNER_TOKEN, "security-export-second"
        );
        String otherJobId = stringField(otherExport, "job_id");
        MvcResult otherJobResult = mvc.perform(get("/v1/jobs/{jobId}", otherJobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(OWNER_TOKEN)))
                .andExpect(status().isOk())
                .andReturn();
        String otherArtifactId = stringField(
                object(otherJobResult), "result_artifact_id"
        );
        mvc.perform(get("/v1/jobs/{jobId}", otherJobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/v1/artifacts/{artifactId}", otherArtifactId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer)))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/v1/access-keys/{keyId}", keyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(OWNER_TOKEN))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, "security-revoke")
                        .header(HttpHeaders.IF_MATCH, quotedEtag(first.contentHash())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revoked").value(true));
        mvc.perform(get("/v1/jobs/{jobId}", ownJobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bearer)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_BEARER_CREDENTIAL"));

    }

    @Test
    void realBearerCommitAndReplayWriteCompleteAuditEvents() throws Exception {
        CanonicalRevision revision = genesis();
        String novelId = stringField(revision.canonicalContent(), "novel_id");
        String revisionId = stringField(revision.canonicalContent(), "revision_id");
        importAsOwner(revision, "commit-audit-import");
        Map<String, Object> key = issueAsOwner(
                revision, "commit-audit-issue", List.of("novel:commit")
        );
        String token = stringField(key, "secret");
        String keyId = stringField(key, "key_id");
        String operationId = Ids.OperationId.create().value();
        String idempotencyKey = "http-commit-audit";
        Map<String, Object> operation = Map.of(
                "operation_id", operationId,
                "idempotency_key", idempotencyKey,
                "novel_id", novelId,
                "base_revision_id", revisionId,
                "expected_head_hash", revision.contentHash(),
                "type", "restore_revision_content",
                "payload", Map.of(
                        "restore_revision_id", revisionId,
                        "expected_restore_hash", revision.contentHash()
                )
        );
        byte[] body = CanonicalJson.bytes(Map.of(
                "operation", operation,
                "candidate_revision_id", Ids.RevisionId.create().value(),
                "candidate_created_at", "2026-08-21T12:01:00Z"
        ));
        MockHttpServletRequestBuilder request = post(
                        "/v1/novels/{novelId}/commits", novelId
                )
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, idempotencyKey)
                .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                .header(ApiRequestMetadata.REQUEST_ID_HEADER, "req_http_commit_first")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        MvcResult first = mvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operation_id").value(operationId))
                .andExpect(jsonPath("$.idempotent_replay").value(false))
                .andReturn();
        String committedRevision = stringField(object(first), "revision_id");

        mvc.perform(post("/v1/novels/{novelId}/commits", novelId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, idempotencyKey)
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .header(ApiRequestMetadata.REQUEST_ID_HEADER, "req_http_commit_retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision_id").value(committedRevision))
                .andExpect(jsonPath("$.idempotent_replay").value(true));

        var commitEvents = store.listAuditEvents(new Ids.NovelId(novelId)).stream()
                .filter(event -> event.action() == AuditAction.COMMIT)
                .toList();
        assertEquals(2, commitEvents.size());
        assertEquals(AuditResult.SUCCEEDED, commitEvents.get(0).result());
        assertEquals(AuditResult.IDEMPOTENT, commitEvents.get(1).result());
        assertEquals("req_http_commit_first", commitEvents.get(0).requestId());
        assertEquals("req_http_commit_retry", commitEvents.get(1).requestId());
        commitEvents.forEach(event -> {
            assertEquals("security-test-actor", event.actorId());
            assertEquals(keyId, event.actorKeyId().value());
            assertEquals(operationId, event.operationId().value());
            assertEquals(committedRevision, event.revisionId().value());
            assertTrue(event.operationHash().startsWith("sha256:"));
            assertTrue(event.contentHash().startsWith("sha256:"));
        });
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
                route("edit preview", "POST", "/v1/novels/nov_test/edit-previews", "novel:propose", false),
                route("undo preview", "POST", "/v1/novels/nov_test/undo-previews", "novel:propose", false),
                route("style analysis", "POST", "/v1/novels/nov_test/style-analyses", "style:analyze", false),
                route("read analysis", "GET", "/v1/style-analyses/analysis_test", "novel:read", false),
                route("rewrite", "POST", "/v1/rewrite-proposals", "rewrite:propose", false),
                route("read rewrite", "GET", "/v1/rewrite-proposals/proposal_test", "novel:read", false),
                route("create profile", "POST", "/v1/style-profiles", "style:admin", true),
                route("profile version", "POST", "/v1/style-profiles/profile_test/versions", "style:admin", false),
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

    private static CanonicalRevision detectorRevision() {
        String novelId = Ids.NovelId.create().value();
        String chapterId = Ids.ChapterId.create().value();
        Map<String, Object> firstBlock = Map.of(
                "id", Ids.BlockId.create().value(),
                "block_version_id", Ids.BlockVersionId.create().value(),
                "order_key", OrderKey.initial().value(),
                "text", "The story starts here.",
                "meta", Map.of()
        );
        Map<String, Object> secondBlock = Map.of(
                "id", Ids.BlockId.create().value(),
                "block_version_id", Ids.BlockVersionId.create().value(),
                "order_key", OrderKey.initial().value(),
                "text", "The story continues here.",
                "meta", Map.of()
        );
        Map<String, Object> firstScene = Map.of(
                "id", Ids.SceneId.create().value(),
                "chapter_id", chapterId,
                "order_key", OrderKey.rebalanced(0, 2).value(),
                "title", "First",
                "transition_mode", "opening",
                "initial_meta", Map.of(
                        "location", Map.of("mode", "explicit", "value", "room_a"),
                        "present_character_ids", List.of()
                ),
                "blocks", List.of(firstBlock)
        );
        Map<String, Object> secondScene = Map.of(
                "id", Ids.SceneId.create().value(),
                "chapter_id", chapterId,
                "order_key", OrderKey.rebalanced(1, 2).value(),
                "title", "Second",
                "transition_mode", "continuous",
                "initial_meta", Map.of(
                        "location", Map.of("mode", "explicit", "value", "room_b"),
                        "present_character_ids", List.of()
                ),
                "blocks", List.of(secondBlock)
        );
        Map<String, Object> chapter = Map.of(
                "id", chapterId,
                "order_key", OrderKey.initial().value(),
                "title", "Detector chapter",
                "scenes", List.of(firstScene, secondScene)
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

    private void importAsOwner(CanonicalRevision revision, String idempotencyKey)
            throws Exception {
        byte[] body = CanonicalJson.bytes(Map.of(
                "format", CanonicalExportFormat.REVISION.canonicalName(),
                "document", revision.envelope()
        ));
        mvc.perform(post("/v1/imports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(OWNER_TOKEN))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, idempotencyKey)
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private Map<String, Object> issueAsOwner(
            CanonicalRevision revision,
            String idempotencyKey,
            List<String> scopes
    ) throws Exception {
        String novelId = stringField(revision.canonicalContent(), "novel_id");
        byte[] body = CanonicalJson.bytes(Map.of(
                "actor_id", "security-test-actor",
                "scopes", scopes,
                "expires_at", "2099-01-01T00:00:00Z"
        ));
        MvcResult result = mvc.perform(post(
                        "/v1/novels/{novelId}/access-keys", novelId
                )
                        .header(HttpHeaders.AUTHORIZATION, bearer(OWNER_TOKEN))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, idempotencyKey)
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(jsonPath("$.secret").isString())
                .andReturn();
        return object(result);
    }

    private Map<String, Object> startExport(
            CanonicalRevision revision,
            String token,
            String idempotencyKey
    ) throws Exception {
        String novelId = stringField(revision.canonicalContent(), "novel_id");
        String revisionId = stringField(revision.canonicalContent(), "revision_id");
        byte[] body = CanonicalJson.bytes(Map.of(
                "revision_id", revisionId,
                "format", CanonicalExportFormat.PACKAGE.canonicalName()
        ));
        MvcResult result = mvc.perform(post(
                        "/v1/novels/{novelId}/exports", novelId
                )
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header(MutationPreconditionFilter.IDEMPOTENCY_KEY, idempotencyKey)
                        .header(HttpHeaders.IF_MATCH, quotedEtag(revision.contentHash()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andReturn();
        return object(result);
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

    @SuppressWarnings("unchecked")
    private static String firstBlockId(CanonicalRevision revision) {
        List<Map<String, Object>> chapters = (List<Map<String, Object>>)
                revision.canonicalContent().get("chapters");
        List<Map<String, Object>> scenes = (List<Map<String, Object>>)
                chapters.getFirst().get("scenes");
        List<Map<String, Object>> blocks = (List<Map<String, Object>>)
                scenes.getFirst().get("blocks");
        return stringField(blocks.getFirst(), "id");
    }

    private static String quotedEtag(String hash) {
        return "\"" + hash + "\"";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor
            userWithScope(String scope) {
        return user("contract-test").authorities(
                new SimpleGrantedAuthority("SCOPE_" + scope)
        );
    }
}
