package dev.storyblock.application;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RegisteredChapters {
  static List<NarrativeChapter> create(AgentNovelRegistration request) {
    List<NarrativeChapter> chapters = new ArrayList<>();
    for (int chapterIndex = 0; chapterIndex < request.chapters().size(); chapterIndex++) {
      AgentNovelRegistration.Chapter draft = request.chapters().get(chapterIndex);
      Ids.ChapterId chapterId = new Ids.ChapterId(StableIds.derive(
          "ch", request.novelId().value(), "chapter:" + chapterIndex
      ));
      Ids.SceneId sceneId = new Ids.SceneId(StableIds.derive(
          "scn", chapterId.value(), "scene:0"
      ));
      List<String> blockTexts = AgentNovelRegistrationServiceBlocks.blocks(draft.text(), chapterIndex);
      List<NarrativeBlock> blocks = new ArrayList<>();
      for (int blockIndex = 0; blockIndex < blockTexts.size(); blockIndex++) {
        Ids.BlockId blockId = new Ids.BlockId(StableIds.derive(
            "blk", sceneId.value(), "block:" + blockIndex
        ));
        blocks.add(new NarrativeBlock(
            blockId,
            new Ids.BlockVersionId(StableIds.derive(
                "blv", blockId.value(), "version:0"
            )),
            OrderKey.rebalanced(blockIndex, blockTexts.size()),
            blockTexts.get(blockIndex),
            BlockMetadata.empty(),
            Map.of("source", "agent-registration")
        ));
      }
      NarrativeScene scene = new NarrativeScene(
          sceneId,
          chapterId,
          OrderKey.initial(),
          draft.title(),
          chapterIndex == 0 ? TransitionMode.OPENING : TransitionMode.TIME_SKIP,
          SceneSeed.empty(),
          blocks,
          Map.of()
      );
      chapters.add(new NarrativeChapter(
          chapterId,
          OrderKey.rebalanced(chapterIndex, request.chapters().size()),
          draft.title(),
          List.of(scene),
          Map.of()
      ));
    }

    return chapters;
  }
}
