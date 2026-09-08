package dev.storyblock.renderer;

import dev.storyblock.domain.DerivedSceneBoundary;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.renderer.DeterministicRenderer.ResolvedEntry;
import static dev.storyblock.renderer.DeterministicRenderer.Resolution;

final class DeterministicRendererResolveAll {
    static Resolution resolveAll(RevisionManifest revision) {
        List<ResolvedEntry> result = new ArrayList<>();
        List<DerivedSceneBoundary> boundaries = new ArrayList<>();
        int sceneIndex = 0;
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                MetadataResolutionState state = MetadataResolutionState.fromSceneSeed(
                        scene.initialMeta()
                );
                var stateIn = state.snapshot();
                for (NarrativeBlock block : scene.blocks()) {
                    var before = state.snapshot();
                    var events = state.apply(block.metadata());
                    var after = state.snapshot();
                    result.add(new ResolvedEntry(
                            sceneIndex,
                            block,
                            new ResolvedBlockMetadata(block.id(), before, events, after)
                    ));
                }
                boundaries.add(new DerivedSceneBoundary(
                        scene.id(), stateIn, state.snapshot()
                ));
                sceneIndex++;
            }
        }
        return new Resolution(List.copyOf(result), List.copyOf(boundaries));
    }
}
