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
import dev.storyblock.domain.UnicodeText;
import dev.storyblock.storage.CanonicalImportResult;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AgentNovelRegistrationService {
    private static final Pattern SENTENCE = Pattern.compile(
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
        validate(request);
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
            List<String> blockTexts = blocks(draft.text(), chapterIndex);
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

    private static void validate(AgentNovelRegistration request) {
        Objects.requireNonNull(request, "request");
        requireText(request.title(), "title", 200);
        requireText(request.language(), "language", 32);
        if (request.mainCharacters().size() != 5
                || new LinkedHashSet<>(request.mainCharacters()).size() != 5) {
            throw new IllegalArgumentException(
                    "main_characters must contain exactly five unique characters"
            );
        }
        request.mainCharacters().forEach(name -> requireText(name, "main character", 80));
        if (request.zombieCount() < 0 || request.tntCannonCount() < 0) {
            throw new IllegalArgumentException("aggregate counts cannot be negative");
        }
        if (request.expectedHanCharacters() < 1) {
            throw new IllegalArgumentException("expected_han_characters must be positive");
        }
        if (request.chapters().isEmpty() || request.chapters().size() > 100) {
            throw new IllegalArgumentException("chapters must contain 1 to 100 entries");
        }
        for (AgentNovelRegistration.Chapter chapter : request.chapters()) {
            Objects.requireNonNull(chapter, "chapter");
            requireText(chapter.title(), "chapter title", 200);
            if (chapter.text() == null || chapter.text().isBlank()) {
                throw new IllegalArgumentException("chapter text cannot be blank");
            }
        }
    }

    private static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(
                    field + " must contain 1 to " + maxLength + " Unicode characters"
            );
        }
    }

    private static List<String> blocks(String text, int chapterIndex) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).strip();
        Matcher matcher = SENTENCE.matcher(normalized);
        List<String> sentences = new ArrayList<>();
        int consumed = 0;
        while (matcher.find()) {
            String sentence = matcher.group(1).strip();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            consumed = matcher.end();
        }
        String trailing = normalized.substring(consumed).strip();
        if (!trailing.isEmpty()) {
            sentences.add(trailing + "。");
        }
        if (sentences.isEmpty()) {
            throw new IllegalArgumentException("chapter " + chapterIndex + " has no sentences");
        }
        for (String sentence : sentences) {
            if (UnicodeText.graphemeCount(sentence) > UnicodeText.MAX_BLOCK_GRAPHEMES) {
                throw new IllegalArgumentException(
                        "chapter " + chapterIndex
                                + " contains a sentence longer than 100 graphemes"
                );
            }
        }
        List<String> result = new ArrayList<>();
        for (int index = 0; index < sentences.size();) {
            String block = sentences.get(index++);
            if (index < sentences.size()) {
                String candidate = block + sentences.get(index);
                if (UnicodeText.graphemeCount(candidate) <= UnicodeText.MAX_BLOCK_GRAPHEMES) {
                    block = candidate;
                    index++;
                }
            }
            UnicodeText.validateBlock(block);
            result.add(block);
        }
        return List.copyOf(result);
    }

    public record Result(
            CanonicalImportResult importResult,
            NovelCatalogEntry novel
    ) {
    }
}
