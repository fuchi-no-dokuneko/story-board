package dev.storyblock.api.http;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/rewrite-proposals")
public final class RewriteProposalController {
    private final RewriteProposalReservation reservations;

    public RewriteProposalController(RewriteProposalReservation reservations) {
        this.reservations = reservations;
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> reserve(
            @RequestBody byte[] request,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication, HttpServletRequest servletRequest) {
        return reservations.reserve(request, ifMatch, idempotencyKey, authentication, servletRequest);
    }
}
