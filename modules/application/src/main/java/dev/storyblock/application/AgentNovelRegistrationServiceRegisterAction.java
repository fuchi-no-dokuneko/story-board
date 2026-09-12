package dev.storyblock.application;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.*;
import dev.storyblock.storage.CanonicalImportResult;
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

    List<NarrativeChapter> chapters = RegisteredChapters.create(request);

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
            "title", request.title()
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
