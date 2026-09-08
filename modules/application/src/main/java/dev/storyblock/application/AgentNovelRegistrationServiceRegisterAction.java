package dev.storyblock.application;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.*;
import dev.storyblock.storage.CanonicalImportResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static dev.storyblock.application.AgentNovelRegistrationService.Result;

final class AgentNovelRegistrationServiceRegisterAction {
  static Result register(AgentNovelRegistrationService self, AgentNovelRegistration request, String idempotencyKey)  {
    AgentNovelRegistrationServiceValidate.validate(request);
    String completeText = request.chapters().stream()
        .map(AgentNovelRegistration.Chapter::text)
        .collect(java.util.stream.Collectors.joining());
    int actualHanCharacters = HanText.count(completeText);
    if (actualHanCharacters != request.expectedHanCharacters()) {
      throw new IllegalArgumentException(
          "Manuscript has " + actualHanCharacters + " Han characters; expected "
              + request.expectedHanCharacters()
      );
    }

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

    NarrativeNovel novel = new NarrativeNovel(
        request.novelId(),
        chapters,
        Map.of(
            "agent-write-registered", true,
            "expected-han-characters", request.expectedHanCharacters(),
            "han-character-count", actualHanCharacters,
            "han-text-sha256", HanText.sha256(completeText),
            "language", request.language(),
            "main-characters", request.mainCharacters(),
            "title", request.title(),
            "tnt-cannon-count", request.tntCannonCount(),
            "zombie-count", request.zombieCount()
        )
    );
    Ids.RevisionId revisionId = new Ids.RevisionId(StableIds.derive(
        "rev", request.novelId().value(), "revision:0"
    ));
    CanonicalRevision canonical = NarrativeCanonicalMapper.toCanonical(
        new RevisionManifest(revisionId, null, request.createdAt(), novel)
    );
    CanonicalImportResult imported = self.transfers.importDocument(
        CanonicalExportFormat.REVISION,
        canonical.envelopeBytes(),
        idempotencyKey,
        request.createdAt()
    );
    return new Result(imported, self.catalog.get(request.novelId()));
  }
}
