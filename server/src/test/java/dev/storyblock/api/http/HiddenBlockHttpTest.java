package dev.storyblock.api.http;

import dev.storyblock.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HiddenBlockHttpTest extends HiddenHttpSupport {
    @Test void rawReadersAndSlicesIncludeNotesButCountsAndRendersExcludeThem() throws Exception {
        var revision = create(); var note = revision.liveBlocks().get(1);
        var admin = mvc.perform(get("/v1/admin/novels/"+revision.novel().id().value()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.novel.block_count").value(3))
                .andReturn().getResponse().getContentAsString();
        assertTrue(admin.contains(HiddenHttpFixture.SENTINEL));
        mvc.perform(get(path(revision)+"/scenes").param("revision_id",revision.id().value()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scenes[0].block_count").value(4))
                .andExpect(jsonPath("$.scenes[0].block_slice.end").value(4))
                .andExpect(jsonPath("$.scenes[0].total_text_graphemes").value(19));
        slice(revision,1,2).andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[0].extensions['storyblock.hidden']").value(true));
        var rendered = postRevision(revision,"/renders",Map.of("revision_id",revision.id().value()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.blocks.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        assertFalse(rendered.contains(HiddenHttpFixture.SENTINEL));
        for (boolean start : List.of(true,false)) {
            var body = Map.<String,Object>of("revision_id",revision.id().value(),
                    "from_block_id",start?note.id().value():revision.liveBlocks().getFirst().id().value(),
                    "to_block_id",start?revision.liveBlocks().getLast().id().value():note.id().value());
            postRevision(revision,"/renders",body).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("HIDDEN_BLOCK_NOT_RENDERABLE"));
        }
    }
}
