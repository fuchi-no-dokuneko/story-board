package dev.storyblock.api.quality;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import dev.storyblock.style.quality.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class NovelQualityService {
    private final SqliteRevisionStore store;
    private final QualityReference reference;
    public NovelQualityService(SqliteRevisionStore store, QualityReference reference) { this.store = store; this.reference = reference; }
    public Map<String, Object> analyze(String novelId, String revisionId, String hash, Set<String> names) {
        var novel = new Ids.NovelId(novelId);
        var revision = store.getRevision(novel, revisionId == null ? store.getHead(novel).revisionId() : new Ids.RevisionId(revisionId));
        if (!revision.contentHash().equals(hash)) throw new IllegalArgumentException("Quality revision hash does not match");
        var text = new StringBuilder();
        var spans = new ArrayList<Map<String,Object>>();
        for (var block : revision.manifest().liveBlocks()) {
            if (!text.isEmpty()) text.append('\n');
            int start = text.length(); text.append(block.text());
            spans.add(Map.of("block_id",block.id().value(),"start",start,"end",text.length()));
        }
        var properNames = new HashSet<>(reference.contract().properNames()); properNames.addAll(names);
        var contract = reference.contract().withNames(properNames);
        var report = new QualityAnalyzer().analyze(text.toString(),contract,reference.calibration(contract));
        return Map.of("novel_id",novelId,"revision_id",revision.manifest().id().value(),"revision_hash",revision.contentHash(),
                "offset_unit","UTF-16","block_spans",spans,"quality_report",report);
    }
}
