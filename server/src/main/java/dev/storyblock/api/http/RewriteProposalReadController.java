package dev.storyblock.api.http;

import dev.storyblock.application.RewriteGateService;
import dev.storyblock.domain.Ids;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/rewrite-proposals")
final class RewriteProposalReadController {
    private final RewriteGateService rewrites;

    RewriteProposalReadController(RewriteGateService rewrites) { this.rewrites = rewrites; }

    @GetMapping("/{proposalId}")
    ResponseEntity<Map<String, Object>> get(@PathVariable String proposalId, Authentication authentication) {
        var reservation = rewrites.get(new Ids.ProposalId(proposalId));
        AccessPrincipalSupport.requireNovel(authentication, reservation.novelId());
        return ResponseEntity.ok().eTag(reservation.reservationHash())
                .body(RewriteProposalView.value(reservation));
    }
}
