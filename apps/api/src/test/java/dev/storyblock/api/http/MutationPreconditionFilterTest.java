package dev.storyblock.api.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MutationPreconditionFilterTest {
    private static final String VALID_ETAG = "\"sha256:"
            + "0".repeat(64)
            + "\"";

    private final MutationPreconditionFilter filter = new MutationPreconditionFilter(
            new ApiProblemWriter()
    );

    @Test
    void rejectsOversizedBodyWhenContentLengthIsUnknown() throws Exception {
        byte[] body = new byte[(int) MutationPreconditionFilter.MAX_REQUEST_BYTES + 1];
        MockHttpServletRequest request = unknownLengthRequest(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"REQUEST_TOO_LARGE\""));
    }

    @Test
    void replaysAcceptedUnknownLengthBodyWithoutChangingBytes() throws Exception {
        byte[] body = "{\"text\":\"content\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = unknownLengthRequest(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<byte[]> observed = new AtomicReference<>();
        AtomicReference<Long> observedLength = new AtomicReference<>();

        filter.doFilter(request, response, (bufferedRequest, ignoredResponse) -> {
            observed.set(bufferedRequest.getInputStream().readAllBytes());
            observedLength.set(bufferedRequest.getContentLengthLong());
        });

        assertArrayEquals(body, observed.get());
        assertEquals(body.length, observedLength.get());
        assertEquals(200, response.getStatus());
    }

    private static MockHttpServletRequest unknownLengthRequest(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/novels/nov_test/commits") {
            @Override
            public int getContentLength() {
                return -1;
            }

            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setRequestURI("/v1/novels/nov_test/commits");
        request.addHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY, "request-1");
        request.addHeader(HttpHeaders.IF_MATCH, VALID_ETAG);
        request.setContent(body);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setAttribute(ApiRequestMetadata.REQUEST_ID_ATTRIBUTE, "req_filter_test");
        return request;
    }

}
