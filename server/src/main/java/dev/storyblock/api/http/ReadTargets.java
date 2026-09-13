package dev.storyblock.api.http;

import dev.storyblock.domain.*;
import java.util.*;

final class ReadTargets {
    final RevisionManifest revision;
    final Map<Ids.SceneId, Set<Integer>> indices = new LinkedHashMap<>();
    ReadTargets(RevisionManifest revision) { this.revision = revision; }

    void range(BlockRangeGuard range) {
        var scene = revision.requireScene(range.sceneId());
        int first = find(scene, range.firstBlockId());
        int last = find(scene, range.lastBlockId());
        add(scene, first - 1, last + 2);
    }

    void point(InsertionPoint point) {
        var scene = revision.requireScene(point.sceneId());
        int index = switch (point.position()) {
            case START -> 0;
            case END -> scene.blocks().size();
            case BEFORE -> find(scene, point.anchorBlockId());
            case AFTER -> find(scene, point.anchorBlockId()) + 1;
        };
        add(scene, index - 1, index + 1);
    }

    void add(NarrativeScene scene, int start, int end) {
        var values = indices.computeIfAbsent(scene.id(), ignored -> new TreeSet<>());
        for (int i = Math.max(0, start); i < Math.min(end, scene.blocks().size()); i++) values.add(i);
    }

    static int find(NarrativeScene scene, Ids.BlockId id) {
        for (int i = 0; i < scene.blocks().size(); i++) if (scene.blocks().get(i).id().equals(id)) return i;
        throw new IllegalArgumentException("Scene does not contain block " + id.value());
    }
}
