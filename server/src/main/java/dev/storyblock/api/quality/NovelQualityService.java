package dev.storyblock.api.quality;

import dev.storyblock.domain.NarrativeView;
import dev.storyblock.domain.Ids;
import dev.storyblock.application.NovelReadCache;
import dev.storyblock.cache.MemoCache;
import dev.storyblock.style.quality.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class NovelQualityService {
    private final NovelReadCache reads;
    private final MemoCache<Key, Map<String,Object>> results = new MemoCache<>(128);
    private final QualityReference reference;
    public NovelQualityService(NovelReadCache reads, QualityReference reference) { this.reads = reads; this.reference = reference; }
    public Map<String, Object> analyze(String novelId, String revisionId, String hash, Set<String> names) {
        var novel = new Ids.NovelId(novelId);
        var read = reads.read(novel, revisionId == null ? null : new Ids.RevisionId(revisionId));
        var revision = read.revision();
        if (!revision.contentHash().equals(hash)) throw new IllegalArgumentException("Quality revision hash does not match");
        var properNames = new HashSet<>(reference.contract().properNames()); properNames.addAll(names);
        var contract = reference.contract().withNames(properNames);
        return results.get(new Key(read.key(), contract), () -> calculate(novelId, read, contract));
    }
    private Map<String,Object> calculate(String novelId, NovelReadCache.Read read, QualityContract contract) {
        var revision = read.revision();
        var text = new StringBuilder();
        var spans = new ArrayList<Map<String,Object>>();
        for (var block : NarrativeView.visibleBlocks(revision.manifest())) {
            if (!text.isEmpty()) text.append('\n');
            int start = text.length(); text.append(block.text());
            spans.add(Map.of("block_id",block.id().value(),"start",start,"end",text.length()));
        }
        var report = new QualityAnalyzer().analyze(text.toString(),contract,reference.calibration(contract));
        return Map.of("novel_id",novelId,"revision_id",revision.manifest().id().value(),"revision_hash",revision.contentHash(),
                "offset_unit","UTF-16","block_spans",spans,"quality_report",report);
    }
    private record Key(NovelReadCache.Key revision, QualityContract contract) { }
}
