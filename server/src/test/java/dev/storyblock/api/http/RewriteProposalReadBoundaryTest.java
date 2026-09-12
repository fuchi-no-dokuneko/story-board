package dev.storyblock.api.http;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import dev.storyblock.application.RewriteGateService;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.*;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RewriteProposalReadBoundaryTest {
    @Test
    void novelBoundReaderCannotReadAnotherNovelsStoredProposal() throws Exception {
        var reservation = RewriteReadFixture.reservation();
        var service = mock(RewriteGateService.class);
        when(service.get(reservation.proposalId())).thenReturn(reservation);
        var mvc = MockMvcBuilders.standaloneSetup(new RewriteProposalReadController(service))
                .setControllerAdvice(new ApiExceptionHandler(true)).build();
        String path = "/v1/rewrite-proposals/" + reservation.proposalId().value();
        mvc.perform(get(path).principal(authentication(reservation.novelId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal_id").value(reservation.proposalId().value()))
                .andExpect(jsonPath("$.novel_id").value(reservation.novelId().value()));
        mvc.perform(get(path).principal(authentication(Ids.NovelId.create())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.worker_input").doesNotExist());
        mvc.perform(get(path).principal(UsernamePasswordAuthenticationToken.authenticated(
                        AccessPrincipal.ownerPrincipal(), null, java.util.List.of())))
                .andExpect(status().isOk());
    }

    private static UsernamePasswordAuthenticationToken authentication(Ids.NovelId novel) {
        var principal = new AccessPrincipal("reader", Ids.AccessKeyId.create(), novel,
                Set.of(AccessScope.NOVEL_READ), Instant.now().plusSeconds(3600), false);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of());
    }
}
