package dev.storyblock.contracts;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NarrativeCanonicalMapper {
    private NarrativeCanonicalMapper() {
    }

    public static CanonicalRevision toCanonical(RevisionManifest manifest) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schema_version", CanonicalRevision.SCHEMA_VERSION);
        document.put("novel_id", manifest.novel().id().value());
        document.put("revision_id", manifest.id().value());
        document.put("parent_revision_id", manifest.parentId() == null ? null : manifest.parentId().value());
        document.put("chapters", manifest.novel().chapters().stream()
                .map(NarrativeCanonicalMapper::chapterToCanonical)
                .toList());
        document.put("created_at", manifest.createdAt().toString());
        putExtensions(document, manifest.novel().extensions());
        return CanonicalRevision.of(document);
    }

    public static RevisionManifest fromCanonical(CanonicalRevision canonical) {
        Map<String, Object> document = canonical.canonicalContent();
        List<NarrativeChapter> chapters = requireList(document.get("chapters"), "chapters").stream()
                .map(value -> chapterFromCanonical(requireMap(value, "chapter")))
                .toList();
        Ids.RevisionId parentId = document.get("parent_revision_id") == null
                ? null
                : new Ids.RevisionId(requireString(document, "parent_revision_id"));
        return new RevisionManifest(
                new Ids.RevisionId(requireString(document, "revision_id")),
                parentId,
                Instant.parse(requireString(document, "created_at")),
                new NarrativeNovel(
                        new Ids.NovelId(requireString(document, "novel_id")),
                        chapters,
                        optionalMap(document.get("extensions"), "extensions")
                )
        );
    }

    private static Map<String, Object> chapterToCanonical(NarrativeChapter chapter) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", chapter.id().value());
        result.put("order_key", chapter.orderKey().value());
        putOptional(result, "title", chapter.title());
        result.put("scenes", chapter.scenes().stream()
                .map(NarrativeCanonicalMapper::sceneToCanonical)
                .toList());
        putExtensions(result, chapter.extensions());
        return result;
    }

    private static Map<String, Object> sceneToCanonical(NarrativeScene scene) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", scene.id().value());
        result.put("chapter_id", scene.chapterId().value());
        result.put("order_key", scene.orderKey().value());
        putOptional(result, "title", scene.title());
        result.put("transition_mode", scene.transitionMode().canonicalName());
        if (scene.initialMeta() != null) {
            result.put("initial_meta", scene.initialMeta().fields());
        }
        result.put("blocks", scene.blocks().stream()
                .map(NarrativeCanonicalMapper::blockToCanonical)
                .toList());
        putExtensions(result, scene.extensions());
        return result;
    }

    private static Map<String, Object> blockToCanonical(NarrativeBlock block) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", block.id().value());
        result.put("block_version_id", block.versionId().value());
        result.put("order_key", block.orderKey().value());
        result.put("text", block.text());
        result.put("meta", block.metadata().fields());
        putExtensions(result, block.extensions());
        return result;
    }

    private static NarrativeChapter chapterFromCanonical(Map<String, Object> chapter) {
        Ids.ChapterId chapterId = new Ids.ChapterId(requireString(chapter, "id"));
        List<NarrativeScene> scenes = requireList(chapter.get("scenes"), "chapter.scenes").stream()
                .map(value -> sceneFromCanonical(chapterId, requireMap(value, "scene")))
                .toList();
        return new NarrativeChapter(
                chapterId,
                new OrderKey(requireString(chapter, "order_key")),
                optionalString(chapter, "title"),
                scenes,
                optionalMap(chapter.get("extensions"), "chapter.extensions")
        );
    }

    private static NarrativeScene sceneFromCanonical(
            Ids.ChapterId parentChapterId,
            Map<String, Object> scene
    ) {
        Ids.ChapterId declaredChapterId = new Ids.ChapterId(requireString(scene, "chapter_id"));
        if (!declaredChapterId.equals(parentChapterId)) {
            throw new IllegalArgumentException("Scene chapter_id does not match its canonical parent");
        }
        List<NarrativeBlock> blocks = requireList(scene.get("blocks"), "scene.blocks").stream()
                .map(value -> blockFromCanonical(requireMap(value, "block")))
                .toList();
        return new NarrativeScene(
                new Ids.SceneId(requireString(scene, "id")),
                declaredChapterId,
                new OrderKey(requireString(scene, "order_key")),
                optionalString(scene, "title"),
                TransitionMode.fromCanonicalName(requireString(scene, "transition_mode")),
                scene.containsKey("initial_meta")
                        ? new SceneSeed(requireMap(scene.get("initial_meta"), "scene.initial_meta"))
                        : null,
                blocks,
                optionalMap(scene.get("extensions"), "scene.extensions")
        );
    }

    private static NarrativeBlock blockFromCanonical(Map<String, Object> block) {
        return new NarrativeBlock(
                new Ids.BlockId(requireString(block, "id")),
                new Ids.BlockVersionId(requireString(block, "block_version_id")),
                new OrderKey(requireString(block, "order_key")),
                requireString(block, "text"),
                new BlockMetadata(requireMap(block.get("meta"), "block.meta")),
                optionalMap(block.get("extensions"), "block.extensions")
        );
    }

    private static void putOptional(Map<String, Object> target, String field, Object value) {
        if (value != null) {
            target.put(field, value);
        }
    }

    private static void putExtensions(Map<String, Object> target, Map<String, Object> extensions) {
        if (!extensions.isEmpty()) {
            target.put("extensions", extensions);
        }
    }

    private static String requireString(Map<String, Object> object, String field) {
        Object value = object.get(field);
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(field + " must be a string");
        }
        return string;
    }

    private static String optionalString(Map<String, Object> object, String field) {
        return object.containsKey(field) ? requireString(object, field) : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireMap(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
        }
        return (Map<String, Object>) map;
    }

    private static Map<String, Object> optionalMap(Object value, String path) {
        return value == null ? Map.of() : requireMap(value, path);
    }

    private static List<Object> requireList(Object value, String path) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        return new ArrayList<>(list);
    }
}
