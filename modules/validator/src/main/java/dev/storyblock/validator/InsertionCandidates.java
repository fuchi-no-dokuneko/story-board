package dev.storyblock.validator;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static dev.storyblock.validator.DeterministicValidator.Candidate;

final class InsertionCandidates {
    static List<Candidate> candidatesAt(DeterministicValidator self, RevisionManifest base, String baseHash, Ids.SceneId sceneId, int insertionIndex, List<BlockDraft> drafts)  {
        NarrativeScene scene = base.requireScene(sceneId);
        Set<String> present = self.presenceBefore(base, baseHash, scene, insertionIndex);
        Map<Ids.BlockId, NarrativeBlock> current = new HashMap<>();
        for (NarrativeBlock block : base.liveBlocks()) {
            current.put(block.id(), block);
        }
        List<Candidate> result = new ArrayList<>();
        for (BlockDraft draft : drafts) {
            NarrativeBlock old = current.get(draft.id());
            result.add(new Candidate(
                    draft.id(),
                    draft.text(),
                    draft.metadata(),
                    Set.copyOf(present),
                    old == null ? null : old.metadata()
            ));
            present = DeterministicValidatorApplyEvents.applyEvents(present, DeterministicValidatorMaps.maps(draft.metadata().fields().get("presence_events")));
        }
        return List.copyOf(result);
    }
}
