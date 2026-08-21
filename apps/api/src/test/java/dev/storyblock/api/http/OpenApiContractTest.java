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
            "POST /novels",
            "POST /imports",
            "GET /novels/{novelId}",
            "GET /novels/{novelId}/revisions/{revisionId}",
            "POST /novels/{novelId}/renders",
            "POST /novels/{novelId}/edit-previews",
            "POST /novels/{novelId}/commits",
            "POST /novels/{novelId}/undo-previews",
            "POST /novels/{novelId}/detector-runs",
            "POST /novels/{novelId}/style-analyses",
            "GET /style-analyses/{analysisId}",
            "POST /rewrite-proposals",
            "GET /rewrite-proposals/{proposalId}",
            "POST /style-profiles",
            "POST /style-profiles/{profileId}/versions",
            "GET /jobs/{jobId}",
            "GET /artifacts/{artifactId}",
            "POST /novels/{novelId}/exports",
            "POST /novels/{novelId}/access-keys",
            "DELETE /access-keys/{keyId}",
            "POST /internal/jobs/claims",
            "POST /internal/jobs/{jobId}/results"
    );
    private static final Set<String> ASYNC_OPERATIONS = Set.of(
            "POST /novels/{novelId}/style-analyses",
            "POST /rewrite-proposals",
            "POST /novels/{novelId}/exports"
    );
    private static final Set<String> WILDCARD_CREATION_OPERATIONS = Set.of(
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
            assertFalse(
                    list(operation.contract().get("x-required-scopes")).isEmpty(),
                    "missing scopes for " + operation.key()
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

        assertStrongEtag("GET /novels/{novelId}", "200");
        assertStrongEtag("GET /novels/{novelId}/revisions/{revisionId}", "200");
        assertStrongEtag("POST /novels", "201");
        assertStrongEtag("POST /imports", "201");
        assertStrongEtag("POST /imports", "200");
        assertStrongEtag("POST /novels/{novelId}/commits", "200");
        assertStrongEtag("POST /novels/{novelId}/commits", "201");
        assertStrongEtag("GET /artifacts/{artifactId}", "200");

        assertTrue(
                parameterRefs(operation("GET /style-analyses/{analysisId}"))
                        .contains("#/components/parameters/Cursor")
        );
    }

    @Test
    void importAndDurableWorkerContractsHaveSchemasAndScopes() {
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
        assertTrue(schemas.containsKey("CanonicalPackage"));
        assertTrue(schemas.containsKey("CanonicalPackageManifest"));
        assertTrue(schemas.containsKey("CanonicalPackageRevision"));
        assertTrue(schemas.containsKey("CanonicalPackageOperation"));
        assertTrue(schemas.containsKey("CanonicalPackageArtifact"));
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
