package dev.storyblock.validator;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.EditOperationValidator;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.List;
import static dev.storyblock.validator.DeterministicValidator.Candidate;

final class DeterministicValidatorCandidatesAction {
    static List<Candidate> candidates(DeterministicValidator self, RevisionManifest base, String baseHash, EditOperation operation)  {
        return switch (operation) {
            case EditOperation.InsertBlocks insert -> self.candidatesAt(
                    base,
                    baseHash,
                    insert.insertionPoint().sceneId(),
                    EditOperationValidator.insertionIndex(
                            base.requireScene(insert.insertionPoint().sceneId()), insert.insertionPoint()
                    ),
                    insert.blocks()
            );
            case EditOperation.ReplaceBlockRange replace -> self.candidatesForRange(
                    base, baseHash, replace.range().sceneId(), replace.range().firstBlockId(), replace.newBlocks()
            );
            case EditOperation.SplitBlock split -> self.candidatesForRange(
                    base, baseHash, split.block().sceneId(), split.block().firstBlockId(), split.newBlocks()
            );
            case EditOperation.MergeBlocks merge -> self.candidatesForRange(
                    base, baseHash, merge.range().sceneId(), merge.range().firstBlockId(), List.of(merge.newBlock())
            );
            case EditOperation.ExtendBlock extend -> self.candidatesForRange(
                    base, baseHash, extend.block().sceneId(), extend.block().firstBlockId(),
                    List.of(extend.replacement())
            );
            case EditOperation.CorrectBlockMeta correction -> {
                NarrativeBlock block = base.requireBlock(correction.block().blockId());
                NarrativeScene scene = base.requireScene(correction.sceneId());
                int index = DeterministicValidatorIndexOf.indexOf(scene.blocks(), block.id());
                yield self.candidatesAt(
                        base,
                        baseHash,
                        scene.id(),
                        index,
                        List.of(new BlockDraft(
                                block.id(), block.text(), correction.correctedMetadata(), block.extensions()
                        ))
                );
            }
            case EditOperation.DeleteBlockRange ignored -> List.of();
            case EditOperation.MoveBlockRange ignored -> List.of();
            case EditOperation.SetSceneInitialMeta ignored -> List.of();
            case EditOperation.RestoreRevisionContent ignored -> List.of();
        };
    }
}
