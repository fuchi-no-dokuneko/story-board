package dev.storyblock.api.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class OpenApiContractTest {
    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete"
    );
    private static final Set<String> REQUIRED_OPERATIONS = Set.of(
            "GET /admin/novels",
            "GET /admin/novels/{novelId}",
            "POST /agent/novels",
            "POST /novels",
            "POST /imports",
            "GET /novels/{novelId}",
            "GET /novels/{novelId}/revisions/{revisionId}",
            "POST /novels/{novelId}/renders",
            "POST /novels/{novelId}/pdf-renders",
            "POST /novels/{novelId}/images",
            "POST /novels/{novelId}/edit-previews",
            "POST /novels/{novelId}/commits",
            "POST /novels/{novelId}/undo-previews",
            "POST /novels/{novelId}/detector-runs",
            "POST /novels/{novelId}/monitor-packets",
            "POST /novels/{novelId}/monitor-runs",
            "GET /novels/{novelId}/monitor-runs/{monitorRunId}",
            "POST /novels/{novelId}/style-analyses",
            "GET /style-analyses/{analysisId}",
            "GET /style-analyses/{analysisId}/windows",
            "POST /rewrite-proposals",
            "GET /rewrite-proposals/{proposalId}",
            "POST /style-profiles",
            "GET /style-profiles/{profileId}",
            "POST /style-profiles/{profileId}/versions",
            "GET /style-profiles/{profileId}/versions/{versionId}",
            "POST /style-profiles/{profileId}/versions/{versionId}/transitions",
            "GET /jobs/{jobId}",
            "GET /artifacts/{artifactId}",
            "POST /novels/{novelId}/exports",
            "POST /novels/{novelId}/access-keys",
            "DELETE /access-keys/{keyId}",
            "POST /internal/jobs/claims",
            "POST /internal/jobs/{jobId}/results"
    );
    private static final Set<String> ASYNC_OPERATIONS = Set.of(
            "POST /rewrite-proposals",
            "POST /novels/{novelId}/exports"
    );
    private static final Set<String> WILDCARD_CREATION_OPERATIONS = Set.of(
            "POST /agent/novels",
            "POST /novels",
            "POST /imports",
            "POST /style-profiles",
            "POST /internal/jobs/claims"
    );

    private static Map<String, Object> document;
    private static List<Operation> operations;

    @BeforeAll
    static void loadDocument() throws IOException {
        try (InputStream stream = OpenApiContractTest.class.getResourceAsStream(
                "/openapi/storyblock-v1.yaml"
        )) {
            assertNotNull(stream, "OpenAPI document must be packaged with the API");
            document = map(new Yaml().load(stream));
        }
        operations = collectOperations(document);
    }

    @Test
    void documentsExactlyTheRequiredV1Operations() {
        Set<String> actual = new HashSet<>();
        Set<String> operationIds = new HashSet<>();

        for (Operation operation : operations) {
            assertTrue(actual.add(operation.key()), "duplicate route " + operation.key());
            String operationId = string(operation.contract().get("operationId"));
            assertFalse(operationId.isBlank(), "missing operationId for " + operation.key());
            assertTrue(operationIds.add(operationId), "duplicate operationId " + operationId);
            assertFalse(
                    string(operation.contract().get("summary")).isBlank(),
                    "missing summary for " + operation.key()
            );
            boolean hasScopes = !list(
                    operation.contract().get("x-required-scopes")
            ).isEmpty();
            Object requiredRole = operation.contract().get("x-required-role");
            boolean hasRole = requiredRole instanceof String role && !role.isBlank();
            assertTrue(
                    hasScopes || hasRole,
                    "missing authorization for " + operation.key()
            );

            Map<String, Object> responses = map(operation.contract().get("responses"));
            assertEquals(
                    "#/components/responses/Problem",
                    map(responses.get("default")).get("$ref"),
                    "default response for " + operation.key()
            );
            assertTrue(
                    responses.keySet().stream().anyMatch(status -> status.startsWith("2")),
                    "missing success response for " + operation.key()
            );

            if (!"GET".equals(operation.method())) {
                Set<String> parameterRefs = parameterRefs(operation.contract());
                String expectedIfMatch = WILDCARD_CREATION_OPERATIONS.contains(
                        operation.key()
                )
                        ? "#/components/parameters/IfMatchForCreation"
                        : "#/components/parameters/IfMatch";
                assertTrue(
                        parameterRefs.contains(expectedIfMatch),
                        "missing If-Match for " + operation.key()
                );
                assertTrue(
                        parameterRefs.contains("#/components/parameters/IdempotencyKey"),
                        "missing Idempotency-Key for " + operation.key()
                );
            }
            if ("POST".equals(operation.method())) {
                assertNotNull(
                        operation.contract().get("requestBody"),
                        "missing request body for " + operation.key()
                );
            }
        }

        assertEquals(REQUIRED_OPERATIONS, actual);
    }

    @Test
    void documentsStatusPolicyAsyncLocationsEtagsAndCursors() {
        Map<String, Object> statusPolicy = map(document.get("x-http-status-policy"));
        Map<Integer, String> documentedStatuses = new HashMap<>();
        statusPolicy.forEach((status, use) -> {
            documentedStatuses.put(Integer.parseInt(status), string(use));
        });
        assertEquals(ApiHttpStatusPolicy.contract(), documentedStatuses);

        for (String operationKey : ASYNC_OPERATIONS) {
            Map<String, Object> responses = map(operation(operationKey).get("responses"));
            assertEquals(
                    "#/components/responses/JobAccepted",
                    map(responses.get("202")).get("$ref"),
                    operationKey
            );
        }
        Map<String, Object> accepted = map(resolve("#/components/responses/JobAccepted"));
        assertEquals(
                "#/components/headers/Location",
                map(map(accepted.get("headers")).get("Location")).get("$ref")
        );
        Map<String, Object> analysisAccepted = map(map(operation(
                "POST /novels/{novelId}/style-analyses"
        ).get("responses")).get("202"));
        assertEquals(
                "#/components/headers/Location",
                map(map(analysisAccepted.get("headers")).get("Location")).get("$ref")
        );
        assertEquals(
                "#/components/schemas/StyleAnalysisAccepted",
                map(map(map(analysisAccepted.get("content")).get("application/json"))
                        .get("schema")).get("$ref")
        );

        assertStrongEtag("GET /novels/{novelId}", "200");
        assertStrongEtag("GET /admin/novels/{novelId}", "200");
        assertStrongEtag("POST /agent/novels", "200");
        assertStrongEtag("POST /agent/novels", "201");
        assertStrongEtag("GET /novels/{novelId}/revisions/{revisionId}", "200");
        assertStrongEtag("POST /novels", "201");
        assertStrongEtag("POST /imports", "201");
        assertStrongEtag("POST /imports", "200");
        assertStrongEtag("POST /novels/{novelId}/renders", "200");
        assertStrongEtag("POST /novels/{novelId}/pdf-renders", "200");
        assertStrongEtag("POST /novels/{novelId}/images", "200");
        assertStrongEtag("POST /novels/{novelId}/images", "201");
        assertStrongEtag("POST /novels/{novelId}/detector-runs", "200");
        assertStrongEtag("POST /novels/{novelId}/monitor-packets", "200");
        assertStrongEtag("POST /novels/{novelId}/monitor-runs", "200");
        assertStrongEtag("POST /novels/{novelId}/monitor-runs", "201");
        assertStrongEtag("GET /novels/{novelId}/monitor-runs/{monitorRunId}", "200");
        assertStrongEtag("POST /style-profiles", "200");
        assertStrongEtag("POST /style-profiles", "201");
        assertStrongEtag("GET /style-profiles/{profileId}", "200");
        assertStrongEtag("POST /style-profiles/{profileId}/versions", "200");
        assertStrongEtag("POST /style-profiles/{profileId}/versions", "201");
        assertStrongEtag("GET /style-profiles/{profileId}/versions/{versionId}", "200");
        assertStrongEtag(
                "POST /style-profiles/{profileId}/versions/{versionId}/transitions",
                "200"
        );
        assertStrongEtag("POST /novels/{novelId}/style-analyses", "202");
        assertStrongEtag("GET /style-analyses/{analysisId}", "200");
        assertStrongEtag("POST /internal/jobs/claims", "200");
        assertStrongEtag("POST /novels/{novelId}/commits", "200");
        assertStrongEtag("POST /novels/{novelId}/commits", "201");
        assertStrongEtag("GET /artifacts/{artifactId}", "200");

        assertTrue(
                parameterRefs(operation("GET /style-analyses/{analysisId}/windows"))
                        .contains("#/components/parameters/Cursor")
        );
        assertTrue(
                parameterRefs(operation("GET /style-analyses/{analysisId}/windows"))
                        .contains("#/components/parameters/PageLimit")
        );
    }

    @Test
    void importAndDurableWorkerContractsHaveSchemasAndScopes() {
        assertEquals(
                "operator",
                string(operation("GET /admin/novels").get("x-required-role"))
        );
        assertEquals(
                "operator",
                string(operation("GET /admin/novels/{novelId}")
                        .get("x-required-role"))
        );
        assertEquals(
                List.of("novel:admin"),
                list(operation("POST /imports").get("x-required-scopes"))
        );
        assertEquals(
                List.of("worker:execute"),
                list(operation("POST /internal/jobs/claims").get("x-required-scopes"))
        );
        assertEquals(
                List.of("worker:execute"),
                list(operation("POST /internal/jobs/{jobId}/results")
                        .get("x-required-scopes"))
        );

        Map<String, Object> schemas = map(map(document.get("components")).get("schemas"));
        assertTrue(schemas.containsKey("ImportRequest"));
        assertTrue(schemas.containsKey("WorkerClaimRequest"));
        assertTrue(schemas.containsKey("WorkerClaimResponse"));
        assertTrue(schemas.containsKey("WorkerResultRequest"));
        assertTrue(schemas.containsKey("WorkerResultResponse"));
        assertTrue(schemas.containsKey("StyleAnalysisSnapshot"));
        assertTrue(schemas.containsKey("StyleAnalysisSummary"));
        assertTrue(schemas.containsKey("StyleAnalysisWindowFinding"));
        assertTrue(schemas.containsKey("CompressedStyleAnalysisTrace"));
        assertTrue(schemas.containsKey("CanonicalPackage"));
        assertTrue(schemas.containsKey("CanonicalPackageManifest"));
        assertTrue(schemas.containsKey("CanonicalPackageRevision"));
        assertTrue(schemas.containsKey("CanonicalPackageOperation"));
        assertTrue(schemas.containsKey("CanonicalPackageArtifact"));
        assertTrue(schemas.containsKey("BlockImage"));
        assertTrue(schemas.containsKey("ImageUploadResponse"));
        assertTrue(schemas.containsKey("PdfRenderRequest"));

        Map<String, Object> claim = map(schemas.get("WorkerClaimRequest"));
        assertEquals(
                Set.of("novel_id", "lease_owner", "lease_seconds"),
                strings(claim.get("required"))
        );
        Map<String, Object> result = map(schemas.get("WorkerResultRequest"));
        assertTrue(strings(result.get("required")).containsAll(Set.of(
                "snapshot_hash", "profile_version_hash",
                "analyzer_contract_hash", "window_configuration_hash",
                "summary", "windows", "trace"
        )));
    }

    @Test
    void accessKeyContractDocumentsOpaqueSecretsAndExactScopes() {
        Map<String, Object> components = map(document.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        Map<String, Object> requestProperties = map(
                map(schemas.get("AccessKeyRequest")).get("properties")
        );
        Map<String, Object> scopeItems = map(
                map(requestProperties.get("scopes")).get("items")
        );
        assertEquals(
                List.of(
                        "novel:read", "novel:analyze", "novel:propose",
                        "novel:commit", "novel:admin", "style:analyze",
                        "style:admin", "rewrite:propose", "monitor:submit",
                        "worker:execute"
                ),
                list(scopeItems.get("enum"))
        );

        Map<String, Object> createdProperties = map(
                map(schemas.get("AccessKeyCreated")).get("properties")
        );
        Map<String, Object> secret = map(createdProperties.get("secret"));
        assertEquals(Boolean.TRUE, secret.get("readOnly"));
        assertEquals(
                "^nv_key_[0-9a-f-]{36}\\.[A-Za-z0-9_-]{43}$",
                secret.get("pattern")
        );

        Map<String, Object> parameters = map(components.get("parameters"));
        assertEquals(
                "^key_[0-9a-f-]{36}$",
                map(map(parameters.get("KeyId")).get("schema")).get("pattern")
        );
    }

    @Test
    void renderPacketDocumentsDeterministicTextOffsetsAndSceneState() {
        Map<String, Object> schemas = map(map(document.get("components")).get("schemas"));
        Map<String, Object> packet = map(schemas.get("RenderPacket"));
        Set<String> required = strings(packet.get("required"));

        assertEquals(Set.of(
                "novel_id", "revision_id", "revision_hash", "renderer_version",
                "range", "rendered_text", "blocks", "resolved_meta",
                "offset_map", "scene_boundaries"
        ), required);
        assertEquals(
                "#/components/schemas/RenderRange",
                map(map(packet.get("properties")).get("range")).get("$ref")
        );
        assertEquals(
                "#/components/schemas/ResolvedSceneBoundary",
                map(map(map(packet.get("properties")).get("scene_boundaries")).get("items"))
                        .get("$ref")
        );

        Map<String, Object> state = map(schemas.get("ResolvedMetadataState"));
        assertEquals(Set.of(
                "time", "location", "weather", "pov", "present_character_ids"
        ), strings(state.get("required")));
    }

    @Test
    void detectorContractDocumentsStableFindingsAndEveryRuleCode() {
        Map<String, Object> schemas = map(map(document.get("components")).get("schemas"));
        Map<String, Object> response = map(schemas.get("DetectorRunResponse"));
        Map<String, Object> finding = map(schemas.get("DetectorFinding"));
        Map<String, Object> properties = map(finding.get("properties"));

        assertEquals(Set.of(
                "finding_id", "code", "severity", "revision_id", "revision_hash",
                "rule_version", "affected_block_ids", "affected_scene_ids",
                "context_block_ids", "evidence"
        ), strings(finding.get("required")));
        assertEquals(Set.of(
                "LOCATION_CHANGED_WITHOUT_TRANSITION",
                "CHARACTER_APPEARED_WITHOUT_ENTER",
                "CHARACTER_DISAPPEARED_WITHOUT_EXIT",
                "WEATHER_CHANGED_WITHOUT_EVIDENCE",
                "TIME_DISCONTINUITY",
                "POV_CHANGED_WITHOUT_BOUNDARY",
                "META_TEXT_MISMATCH",
                "INTENTIONAL_SCENE_RESET"
        ), strings(map(properties.get("code")).get("enum")));
        assertEquals(
                Set.of("error", "warning", "info"),
                strings(map(properties.get("severity")).get("enum"))
        );
        assertEquals(Boolean.TRUE, map(properties.get("affected_block_ids"))
                .get("uniqueItems"));
        assertEquals(Boolean.TRUE, map(properties.get("affected_scene_ids"))
                .get("uniqueItems"));
        assertEquals(3, map(properties.get("context_block_ids")).get("maxItems"));
        assertEquals(
                "#/components/schemas/DetectorFinding",
                map(map(map(response.get("properties")).get("findings")).get("items"))
                        .get("$ref")
        );
    }

    @Test
    void monitorContractIsBoundedEvidenceOnlyAndNeverRebases() {
        assertEquals(
                List.of("novel:read"),
                list(operation("POST /novels/{novelId}/monitor-packets")
                        .get("x-required-scopes"))
        );
        assertEquals(
                List.of("monitor:submit"),
                list(operation("POST /novels/{novelId}/monitor-runs")
                        .get("x-required-scopes"))
        );
        Map<String, Object> schemas = map(map(document.get("components")).get("schemas"));
        Map<String, Object> invariants = map(schemas.get("MonitorLocalInvariants"));
        Map<String, Object> window = map(map(invariants.get("properties")).get("window_blocks"));
        assertEquals(5, window.get("maxItems"));

        Map<String, Object> packet = map(schemas.get("MonitorPacket"));
        Map<String, Object> tools = map(map(packet.get("properties")).get("allowed_tools"));
        List<?> toolSlots = list(tools.get("prefixItems"));
        assertEquals("submit_finding", map(toolSlots.get(0)).get("const"));
        assertEquals("submit_proposed_operation", map(toolSlots.get(1)).get("const"));

        Map<String, Object> status = map(schemas.get("MonitorRunStatus"));
        assertEquals(
                Boolean.FALSE,
                map(map(status.get("properties")).get("rebase_allowed")).get("const")
        );
        assertTrue(schemas.containsKey("MonitorEvidence"));
        assertTrue(schemas.containsKey("MonitorFindingOutput"));
        assertTrue(schemas.containsKey("MonitorProposedOperationOutput"));
    }

    @Test
    void styleContractMakesPromotionExplicitAndKlDiagnosticOnly() {
        assertEquals(
                List.of("style:admin"),
                list(operation("POST /style-profiles/{profileId}/versions/{versionId}/transitions")
                        .get("x-required-scopes"))
        );
        Map<String, Object> schemas = map(map(document.get("components")).get("schemas"));
        Map<String, Object> transition = map(schemas.get("StyleProfileTransitionRequest"));
        assertTrue(strings(transition.get("required"))
                .contains("confirm_generated_corpus_promotion"));

        Map<String, Object> featureSet = map(schemas.get("StyleFeatureSet"));
        assertEquals(5, map(map(featureSet.get("properties")).get("channels"))
                .get("minItems"));
        Map<String, Object> report = map(schemas.get("StyleDistanceReport"));
        assertEquals(
                Boolean.TRUE,
                map(map(report.get("properties")).get("token_kl_diagnostic_only"))
                        .get("const")
        );
        Object primaryMetrics = map(map(map(
                schemas.get("StyleChannelDistance")
        ).get("properties")).get("primary_metric")).get("enum");
        assertTrue(strings(primaryMetrics).stream().noneMatch(metric ->
                metric.contains("kl")
        ));
        assertTrue(schemas.containsKey("StyleWindow"));
        assertTrue(schemas.containsKey("StyleCalibrationProfile"));
        assertTrue(schemas.containsKey("StyleWindowScore"));
        Map<String, Object> decision = map(schemas.get("StyleAnomalyDecision"));
        assertEquals(Set.of(
                "normal", "warning", "rewrite_candidate", "topic_shift_only",
                "low_confidence"
        ), strings(map(map(decision.get("properties")).get("state")).get("enum")));
        assertTrue(string(map(map(decision.get("properties"))
                .get("localized_micro_window_ids")).get("description"))
                .contains("never sufficient"));
        assertEquals(5, map(map(decision.get("properties"))
                .get("independent_q99_channels")).get("maxItems"));
    }

    @Test
    void everyLocalReferenceResolves() {
        walkReferences(document, "#");
    }

    private static void assertStrongEtag(String operationKey, String status) {
        Map<String, Object> response = map(map(operation(operationKey).get("responses"))
                .get(status));
        assertEquals(
                "#/components/headers/StrongEtag",
                map(map(response.get("headers")).get("ETag")).get("$ref"),
                operationKey + " " + status
        );
    }

    private static Map<String, Object> operation(String key) {
        return operations.stream()
                .filter(operation -> operation.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing operation " + key))
                .contract();
    }

    private static Set<String> parameterRefs(Map<String, Object> operation) {
        Set<String> refs = new HashSet<>();
        for (Object parameter : list(operation.get("parameters"))) {
            Object ref = map(parameter).get("$ref");
            if (ref instanceof String value) {
                refs.add(value);
            }
        }
        return refs;
    }

    private static List<Operation> collectOperations(Map<String, Object> root) {
        List<Operation> result = new ArrayList<>();
        map(root.get("paths")).forEach((path, pathValue) -> {
            Map<String, Object> pathItem = map(pathValue);
            HTTP_METHODS.forEach(method -> {
                if (pathItem.containsKey(method)) {
                    result.add(new Operation(
                            method.toUpperCase(), path, map(pathItem.get(method))
                    ));
                }
            });
        });
        return List.copyOf(result);
    }

    private static void walkReferences(Object value, String location) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> current = map(rawMap);
            Object ref = current.get("$ref");
            if (ref instanceof String reference) {
                assertTrue(reference.startsWith("#/"), "external reference at " + location);
                assertNotNull(resolve(reference), "unresolved reference " + reference);
            }
            current.forEach((key, child) -> walkReferences(child, location + "/" + key));
        } else if (value instanceof List<?> values) {
            for (int index = 0; index < values.size(); index++) {
                walkReferences(values.get(index), location + "/" + index);
            }
        }
    }

    private static Object resolve(String reference) {
        Object current = document;
        for (String rawSegment : reference.substring(2).split("/")) {
            String segment = rawSegment.replace("~1", "/").replace("~0", "~");
            Map<String, Object> parent = map(current);
            if (!parent.containsKey(segment)) {
                fail("unresolved reference " + reference + " at " + segment);
            }
            current = parent.get(segment);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            throw new AssertionError("expected map, got " + value);
        }
        return (Map<String, Object>) value;
    }

    private static List<?> list(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new AssertionError("expected list, got " + value);
        }
        return values;
    }

    private static String string(Object value) {
        if (!(value instanceof String text)) {
            throw new AssertionError("expected string, got " + value);
        }
        return text;
    }

    private static Set<String> strings(Object value) {
        Set<String> result = new HashSet<>();
        for (Object entry : list(value)) {
            result.add(string(entry));
        }
        return result;
    }

    private record Operation(
            String method,
            String path,
            Map<String, Object> contract
    ) {
        String key() {
            return method + " " + path;
        }
    }
}
