package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HiddenStyleRangeHttpTest extends HiddenHttpSupport {
    @Autowired SqliteRevisionStore store;
    @Test void styleRangeRejectsHiddenEndpointsAndSnapshotContainsOnlyProse() throws Exception {
        var revision = create(); var profile = HiddenStyleFixture.ready(store,revision).profileVersion();
        var body = new HashMap<String,Object>(Map.of("revision_id",revision.id().value(),
                "profile_id",profile.profileId().value(),"profile_version_id",profile.versionId().value()));
        var note = revision.liveBlocks().get(1); var first = revision.liveBlocks().getFirst();
        for (boolean start : List.of(true,false)) {
            body.put("from_block_id",start?note.id().value():first.id().value());
            body.put("to_block_id",start?first.id().value():note.id().value());
            postRevision(revision,"/style-analyses",body).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("HIDDEN_BLOCK_NOT_RENDERABLE"));
        }
        body.remove("from_block_id");body.remove("to_block_id");
        var result = postRevision(revision,"/style-analyses",body).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsByteArray();
        var response = CanonicalJson.parse(result,Map.class);
        var job = store.getStyleAnalysis(new dev.storyblock.domain.Ids.StyleAnalysisId((String)response.get("analysis_id")));
        assertEquals(3,job.snapshot().blocks().size());
        assertFalse(CanonicalJson.string(job.snapshot().canonicalValue()).contains(HiddenHttpFixture.SENTINEL));
    }
}
