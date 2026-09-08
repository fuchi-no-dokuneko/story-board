package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.StableIds;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.storage.CanonicalImportResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class AgentNovelRegistrationService {
    static final Pattern SENTENCE = Pattern.compile(
            "\\G\\s*(.*?(?:[。！？!?]+|…{1,2})[」』”’\\\"'）)】》〉〕］}]*?)",
            Pattern.DOTALL
    );

    private final CanonicalTransferService transfers;
    private final NovelCatalogService catalog;

    public AgentNovelRegistrationService(
            CanonicalTransferService transfers,
            NovelCatalogService catalog
    ) {
        this.transfers = Objects.requireNonNull(transfers, "transfers");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public Result register(
            AgentNovelRegistration request,
            String idempotencyKey
    ) {
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
        CanonicalImportResult imported = transfers.importDocument(
                CanonicalExportFormat.REVISION,
                canonical.envelopeBytes(),
                idempotencyKey,
                request.createdAt()
        );
        return new Result(imported, catalog.get(request.novelId()));
    }

    public record Result(
            CanonicalImportResult importResult,
            NovelCatalogEntry novel
    ) {
    }
}
