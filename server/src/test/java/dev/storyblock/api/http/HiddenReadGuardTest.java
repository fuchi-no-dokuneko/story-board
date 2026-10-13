package dev.storyblock.api.http;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HiddenReadGuardTest extends HiddenHttpSupport {
    @Test void editingNextToNoteRequiresReadingItAndCommitsTheHiddenWireDraft() throws Exception {
        var revision = create(); var scene = revision.novel().chapters().getFirst().scenes().getFirst();
        var context = new EditContext(Ids.OperationId.create(),UUID.randomUUID().toString(),
                revision.novel().id(),revision.id(),hash(revision));
        var note = new BlockDraft(Ids.BlockId.create(),"Unstated motive\nPayoff in chapter 9.",
                BlockMetadata.empty(),Map.of(BlockHidden.EXTENSION_KEY,true));
        var operation = new EditOperation.InsertBlocks(context,
                InsertionPoint.after(scene.id(),scene.blocks().getFirst().id()),List.of(note));
        var body = Map.<String,Object>of("operation",EditOperationCanonicalMapper.toCanonical(operation),
                "candidate_revision_id",Ids.RevisionId.create().value(),"candidate_created_at",Instant.now().toString());
        slice(revision,0,1).andExpect(status().isOk());
        slice(revision,2,3).andExpect(status().isOk());
        commit(revision,context,body).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECENT_READ_REQUIRED"))
                .andExpect(jsonPath("$.missing_block_ids[0]").value(scene.blocks().get(1).id().value()));
        slice(revision,1,2).andExpect(status().isOk());
        postRevision(revision,"/edit-previews",body).andExpect(status().isOk())
                .andExpect(jsonPath("$.committable").value(true));
        commit(revision,context,body).andExpect(status().isCreated());
        var result = mvc.perform(get("/v1/admin/novels/"+revision.novel().id().value())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(result.contains(note.text().split("\n")[0]));
        assertTrue(result.contains(note.id().value()));
    }
    private org.springframework.test.web.servlet.ResultActions commit(RevisionManifest revision,EditContext context,
            Map<String,Object> body) throws Exception {
        return mvc.perform(post(path(revision)+"/commits").header("If-Match",'"'+hash(revision)+'"')
                .header("Idempotency-Key",context.idempotencyKey()).contentType("application/json")
                .content(CanonicalJson.bytes(body)));
    }
}
