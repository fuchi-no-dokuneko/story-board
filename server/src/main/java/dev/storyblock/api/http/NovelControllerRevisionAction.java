package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StoredRevision;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class NovelControllerRevisionAction {
    static ResponseEntity<Map<String, Object>> revision(NovelController self, String novelId, String revisionId, Authentication authentication)  {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        StoredRevision stored = self.transfers.getRevision(
                requestedNovel, new Ids.RevisionId(revisionId)
        );
        CanonicalRevision canonical = NarrativeCanonicalMapper.toCanonical(
                stored.manifest()
        );
        return ResponseEntity.ok().eTag(canonical.contentHash()).body(
                canonical.envelope()
        );
    }
}
