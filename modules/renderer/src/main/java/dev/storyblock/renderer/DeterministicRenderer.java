package dev.storyblock.renderer;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class DeterministicRenderer {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final List<String> RESOLVED_FIELDS = List.of(
            "time", "location", "weather", "pov"
    );

    public RenderPacket render(
            RevisionManifest revision,
            String revisionHash,
            RenderRange requestedRange
    ) {
        Objects.requireNonNull(revision, "revision");
        if (revisionHash == null || !SHA_256.matcher(revisionHash).matches()) {
            throw new IllegalArgumentException("Revision hash must be lowercase SHA-256");
        }
        Objects.requireNonNull(requestedRange, "requestedRange");

        List<ResolvedEntry> resolved = resolveAll(revision);
        if (resolved.isEmpty()) {
            if (!requestedRange.isAll()) {
                throw new IllegalArgumentException("An empty revision has no render endpoints");
            }
            return new RenderPacket(
                    revision.novel().id(),
                    revision.id(),
                    revisionHash,
                    RendererModule.VERSION,
                    RenderRange.all(),
                    "",
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        int from = requestedRange.isAll() ? 0 : indexOf(resolved, requestedRange.fromBlockId());
        int to = requestedRange.isAll()
                ? resolved.size() - 1
                : indexOf(resolved, requestedRange.toBlockId());
        if (from > to) {
            throw new IllegalArgumentException("Render range endpoints are reversed");
        }

        List<RenderedBlock> blocks = new ArrayList<>();
        List<ResolvedBlockMetadata> metadata = new ArrayList<>();
        List<OffsetMapEntry> offsets = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (int index = from; index <= to; index++) {
            ResolvedEntry entry = resolved.get(index);
            if (!blocks.isEmpty()) {
                text.append('\n');
            }
            int start = text.codePointCount(0, text.length());
            text.append(entry.block().text());
            int end = text.codePointCount(0, text.length());
            blocks.add(new RenderedBlock(
                    entry.block().id(),
                    entry.block().versionId(),
                    entry.block().text(),
                    entry.block().metadata()
            ));
            metadata.add(entry.resolved());
            offsets.add(new OffsetMapEntry(entry.block().id(), start, end));
        }

        RenderRange actualRange = blocks.isEmpty()
                ? RenderRange.all()
                : RenderRange.inclusive(blocks.getFirst().blockId(), blocks.getLast().blockId());
        return new RenderPacket(
                revision.novel().id(),
                revision.id(),
                revisionHash,
                RendererModule.VERSION,
                actualRange,
                text.toString(),
                blocks,
                metadata,
                offsets
        );
    }

    private static List<ResolvedEntry> resolveAll(RevisionManifest revision) {
        List<ResolvedEntry> result = new ArrayList<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                Map<String, Object> state = initialState(scene);
                for (NarrativeBlock block : scene.blocks()) {
                    Map<String, Object> before = snapshot(state);
                    applyObservations(state, block.metadata().fields());
                    List<Map<String, Object>> events = presenceEvents(block.metadata().fields());
                    applyPresenceEvents(state, events);
                    Map<String, Object> after = snapshot(state);
                    result.add(new ResolvedEntry(
                            block,
                            new ResolvedBlockMetadata(block.id(), before, events, after)
                    ));
                }
            }
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> initialState(NarrativeScene scene) {
        Map<String, Object> state = new LinkedHashMap<>();
        if (scene.initialMeta() == null) {
            state.put("present_character_ids", List.of());
            return state;
        }
        Map<String, Object> seed = scene.initialMeta().fields();
        for (String field : List.of("time", "location", "weather")) {
            if (seed.containsKey(field)) {
                applyObservation(state, field, seed.get(field));
            }
        }
        state.put("present_character_ids", sortedStrings(seed.get("present_character_ids")));
        return state;
    }

    private static void applyObservations(Map<String, Object> state, Map<String, Object> metadata) {
        for (String field : RESOLVED_FIELDS) {
            if (metadata.containsKey(field)) {
                applyObservation(state, field, metadata.get(field));
            }
        }
    }

    private static void applyObservation(Map<String, Object> state, String field, Object observation) {
        if (!(observation instanceof Map<?, ?> map) || !(map.get("mode") instanceof String mode)) {
            state.put(field, observation);
            return;
        }
        switch (mode) {
            case "explicit" -> state.put(field, map.get("value"));
            case "inherited" -> {
                // Keep the prior local value exactly as-is.
            }
            case "unknown", "not_applicable" -> state.put(field, Map.of("mode", mode));
            default -> state.put(field, observation);
        }
    }

    private static void applyPresenceEvents(
            Map<String, Object> state,
            List<Map<String, Object>> events
    ) {
        Set<String> present = new TreeSet<>(sortedStrings(state.get("present_character_ids")));
        for (Map<String, Object> event : events) {
            Object type = event.get("type");
            Object character = event.get("character_id");
            if (!(character instanceof String characterId)) {
                continue;
            }
            if ("enter".equals(type)) {
                present.add(characterId);
            } else if ("exit".equals(type)) {
                present.remove(characterId);
            }
        }
        state.put("present_character_ids", List.copyOf(present));
    }

    private static List<Map<String, Object>> presenceEvents(Map<String, Object> metadata) {
        Object value = metadata.get("presence_events");
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<Map<String, Object>> events = new ArrayList<>();
        for (Object entry : values) {
            if (entry instanceof Map<?, ?> map) {
                Map<String, Object> event = new LinkedHashMap<>();
                for (Map.Entry<?, ?> field : map.entrySet()) {
                    if (field.getKey() instanceof String key) {
                        event.put(key, field.getValue());
                    }
                }
                events.add(event);
            }
        }
        return List.copyOf(events);
    }

    private static List<String> sortedStrings(Object value) {
        if (!(value instanceof Collection<?> values)) {
            return List.of();
        }
        Set<String> sorted = new TreeSet<>();
        for (Object entry : values) {
            if (entry instanceof String string) {
                sorted.add(string);
            }
        }
        return List.copyOf(sorted);
    }

    private static Map<String, Object> snapshot(Map<String, Object> state) {
        Map<String, Object> snapshot = new LinkedHashMap<>(state);
        snapshot.put(
                "present_character_ids",
                List.copyOf(new LinkedHashSet<>(sortedStrings(state.get("present_character_ids"))))
        );
        return CanonicalValues.freezeMap(snapshot, "resolved_state");
    }

    private static int indexOf(List<ResolvedEntry> entries, Ids.BlockId blockId) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).block().id().equals(blockId)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Revision does not contain render endpoint " + blockId.value());
    }

    private record ResolvedEntry(NarrativeBlock block, ResolvedBlockMetadata resolved) {
    }
}
