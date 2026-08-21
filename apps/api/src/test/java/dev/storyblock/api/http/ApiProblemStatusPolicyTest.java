package dev.storyblock.api.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiProblemStatusPolicyTest {
    private static final Set<Integer> REQUIRED_STATUSES = Set.of(
            200, 201, 202,
            400, 401, 403, 404, 409, 410, 412, 413, 422, 428, 429, 503
    );

    @Test
    void statusPolicyContainsEveryRequiredStatusAndNoExtras() {
        assertEquals(REQUIRED_STATUSES, ApiHttpStatusPolicy.contract().keySet());
        ApiHttpStatusPolicy.contract().values().forEach(use -> assertTrue(!use.isBlank()));
    }

    @Test
    void everyErrorStatusCanProduceTheStableProblemShape() {
        for (Integer statusValue : REQUIRED_STATUSES) {
            if (statusValue < 400) {
                continue;
            }
            HttpStatus status = HttpStatus.valueOf(statusValue);
            MockHttpServletRequest request = request("/v1/status/" + statusValue);
            ApiFailureException failure = new ApiFailureException(
                    status,
                    "STATUS_" + statusValue,
                    "Status " + statusValue,
                    "status-" + statusValue,
                    "Contract status " + statusValue,
                    Map.of("policy_status", statusValue),
                    null
            );

            Map<String, Object> problem = ApiProblemFactory.create(request, failure);

            assertEquals(statusValue, problem.get("status"), "status " + statusValue);
            assertEquals("STATUS_" + statusValue, problem.get("code"));
            assertEquals("Contract status " + statusValue, problem.get("detail"));
            assertEquals("/v1/status/" + statusValue, problem.get("instance"));
            assertEquals("req_status_test", problem.get("request_id"));
            assertEquals(statusValue, problem.get("policy_status"));
        }
    }

    @Test
    void rejectsSuccessStatusesAndReservedProblemExtensions() {
        MockHttpServletRequest request = request("/v1/test");
        assertThrows(IllegalArgumentException.class, () -> new ApiFailureException(
                HttpStatus.OK,
                "NOT_AN_ERROR",
                "Not an error",
                "not-an-error",
                "Success is not a problem.",
                Map.of(),
                null
        ));

        ApiFailureException collision = new ApiFailureException(
                HttpStatus.CONFLICT,
                "TEST_CONFLICT",
                "Conflict",
                "test-conflict",
                "Reserved extension collision.",
                Map.of("status", 999),
                null
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ApiProblemFactory.create(request, collision)
        );
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        request.setAttribute(
                ApiRequestMetadata.REQUEST_ID_ATTRIBUTE,
                "req_status_test"
        );
        return request;
    }
}
