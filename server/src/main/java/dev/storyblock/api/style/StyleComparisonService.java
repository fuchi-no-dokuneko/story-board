package dev.storyblock.api.style;

import dev.storyblock.api.http.ApiFailureException;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import dev.storyblock.style.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class StyleComparisonService {
    private final StyleLibrary library;
    private final SqliteRevisionStore store;
    public StyleComparisonService(StyleLibrary library, SqliteRevisionStore store) {
        this.library = library; this.store = store;
    }

    public Map<String, Object> compare(String novelId, String revisionId, String expectedHash, List<String> ids) {
        if (ids.isEmpty() || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException("style_ids must be a nonempty distinct list");
        var snapshot = library.snapshot();
        if (snapshot.registryError() != null) throw ApiFailureException.unavailable(snapshot.registryError());
        var novel = new Ids.NovelId(novelId);
        var revision = store.getRevision(novel, revisionId == null ? store.getHead(novel).revisionId() : new Ids.RevisionId(revisionId));
        if (!revision.contentHash().equals(expectedHash)) throw new IllegalArgumentException("Comparison revision hash does not match");
        var analyzer = new StyleFeatureAnalyzer();
        var lexicon = StyleMaskingLexicon.empty();
        var features = analyzer.extract(revision.manifest().liveBlocks(), lexicon,
                StyleFeatureContract.defaults(lexicon.vocabularyHash()));
        var results = new ArrayList<Map<String, Object>>();
        for (String id : ids) {
            if (snapshot.definitions().stream().noneMatch(style -> style.id().equals(id)))
                throw new IllegalArgumentException("Unknown style: " + id);
            var baseline = snapshot.benchmarks().get(id);
            if (baseline == null || snapshot.errors().containsKey(id))
                throw ApiFailureException.unavailable("Reference benchmark unavailable: " + id);
            var value = new LinkedHashMap<>(baseline.publicValue(null));
            value.put("scores", analyzer.compare(baseline.features(), features).channels().stream()
                    .map(StyleChannelDistance::canonicalValue).toList());
            value.put("reference_metadata", "text_only");
            results.add(value);
        }
        return Map.of("novel_id", novelId, "revision_id", revision.manifest().id().value(),
                "revision_hash", revision.contentHash(), "comparisons", results);
    }
}
