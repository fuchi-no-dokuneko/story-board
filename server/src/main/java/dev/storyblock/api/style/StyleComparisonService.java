package dev.storyblock.api.style;

import dev.storyblock.api.http.ApiFailureException;
import dev.storyblock.domain.Ids;
import dev.storyblock.application.NovelReadCache;
import dev.storyblock.cache.MemoCache;
import dev.storyblock.style.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class StyleComparisonService {
    private final StyleLibrary library;
    private final NovelReadCache reads;
    private final MemoCache<Key,Map<String,Object>> results = new MemoCache<>(128);
    public StyleComparisonService(StyleLibrary library, NovelReadCache reads) {
        this.library = library; this.reads = reads;
    }

    public Map<String, Object> compare(String novelId, String revisionId, String expectedHash, List<String> ids) {
        if (ids.isEmpty() || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException("style_ids must be a nonempty distinct list");
        var snapshot = library.snapshot();
        if (snapshot.registryError() != null) throw ApiFailureException.unavailable(snapshot.registryError());
        var novel = new Ids.NovelId(novelId);
        var read = reads.read(novel, revisionId == null ? null : new Ids.RevisionId(revisionId));
        var revision = read.revision();
        if (!revision.contentHash().equals(expectedHash)) throw new IllegalArgumentException("Comparison revision hash does not match");
        var baselines = new ArrayList<StyleBenchmark>();
        for (String id : ids) {
            if (snapshot.definitions().stream().noneMatch(style -> style.id().equals(id)))
                throw new IllegalArgumentException("Unknown style: " + id);
            var baseline = snapshot.benchmarks().get(id);
            if (baseline == null || snapshot.errors().containsKey(id))
                throw ApiFailureException.unavailable("Reference benchmark unavailable: " + id);
            baselines.add(baseline);
        }
        var key = new Key(read.key(), List.copyOf(baselines));
        return results.get(key, () -> StyleComparisonCalculation.calculate(novelId, read, key.styles()));
    }

    private record Key(NovelReadCache.Key revision, List<StyleBenchmark> styles) { }
}
