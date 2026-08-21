package dev.storyblock.detector;

import dev.storyblock.domain.DerivedSceneBoundary;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.renderer.DeterministicRenderer;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.renderer.RenderedBlock;
import dev.storyblock.renderer.ResolvedBlockMetadata;
import dev.storyblock.validator.EvidenceSpans;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class AdjacentMetadataDetector {
    private static final List<String> OBSERVATION_FIELDS = List.of(
            "location", "weather", "time", "pov"
    );

    private final DeterministicRenderer renderer;

    public AdjacentMetadataDetector() {
        this(new DeterministicRenderer());
    }

    public AdjacentMetadataDetector(DeterministicRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public DetectorRun detect(
            RevisionManifest revision,
            String revisionHash,
            RenderRange requestedRange
    ) {
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(requestedRange, "requestedRange");

        RenderPacket packet = renderer.render(revision, revisionHash, RenderRange.all());
        List<NarrativeScene> scenes = scenes(revision);
        List<BlockContext> blocks = blockContexts(scenes, packet);
        Selection selection = select(blocks, scenes, requestedRange);
        List<DetectorFinding> findings = new ArrayList<>();

        for (int index = selection.fromBlock(); index <= selection.toBlock(); index++) {
            inspectBlock(revision, packet, blocks, index, findings);
        }
        inspectBoundaries(revision, packet, scenes, selection, findings);

        return new DetectorRun(
                revision.id(),
                revisionHash,
                DetectorModule.VERSION,
                List.copyOf(findings)
        );
    }

    private static void inspectBlock(
            RevisionManifest revision,
            RenderPacket packet,
            List<BlockContext> blocks,
            int index,
            List<DetectorFinding> findings
    ) {
        BlockContext current = blocks.get(index);
        Map<String, Object> local = current.rendered().localMetadata().fields();
        List<Ids.BlockId> contextIds = contextBlockIds(blocks, index);

        inspectClaimedEvidence(revision, packet, current, contextIds, findings);
        inspectPresenceDelta(revision, packet, current, contextIds, findings);

        boolean transitionBlock = "transition".equals(local.get("narrative_mode"));
        inspectFieldChange(
                revision,
                packet,
                current,
                contextIds,
                "location",
                FindingCode.LOCATION_CHANGED_WITHOUT_TRANSITION,
                !transitionBlock && !hasValidEvidence(local.get("location"), local, current.text()),
                findings
        );
        inspectFieldChange(
                revision,
                packet,
                current,
                contextIds,
                "weather",
                FindingCode.WEATHER_CHANGED_WITHOUT_EVIDENCE,
                !hasValidEvidence(local.get("weather"), local, current.text()),
                findings
        );
        inspectFieldChange(
                revision,
                packet,
                current,
                contextIds,
                "time",
                FindingCode.TIME_DISCONTINUITY,
                !transitionBlock && !hasValidEvidence(local.get("time"), local, current.text()),
                findings
        );
        inspectFieldChange(
                revision,
                packet,
                current,
                contextIds,
                "pov",
                FindingCode.POV_CHANGED_WITHOUT_BOUNDARY,
                !current.firstInScene() && !transitionBlock,
                findings
        );
    }

    private static void inspectFieldChange(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            String field,
            FindingCode code,
            boolean report,
            List<DetectorFinding> findings
    ) {
        Object before = current.resolved().before().get(field);
        Object after = current.resolved().after().get(field);
        Object local = current.rendered().localMetadata().fields().get(field);
        if (!report || !isExplicit(local) || !comparableChange(before, after)) {
            return;
        }

        addFinding(
                revision,
                packet,
                code,
                List.of(current.blockId()),
                List.of(current.scene().id()),
                contextIds,
                Map.of(
                        "after", after,
                        "before", before,
                        "field", field,
                        "kind", "block_transition"
                ),
                findings
        );
    }

    private static void inspectClaimedEvidence(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            List<DetectorFinding> findings
    ) {
        Map<String, Object> local = current.rendered().localMetadata().fields();
        for (String field : OBSERVATION_FIELDS) {
            Object observation = local.get(field);
            if (observation instanceof Map<?, ?> map && map.containsKey("evidence")
                    && !matchesEvidence(current.text(), map.get("evidence"))) {
                addEvidenceMismatch(
                        revision,
                        packet,
                        current,
                        contextIds,
                        field + ".evidence",
                        findings
                );
            }
        }

        Object events = local.get("presence_events");
        if (events instanceof List<?> entries) {
            for (int index = 0; index < entries.size(); index++) {
                Object entry = entries.get(index);
                if (entry instanceof Map<?, ?> event && event.containsKey("evidence")
                        && !matchesEvidence(current.text(), event.get("evidence"))) {
                    addEvidenceMismatch(
                            revision,
                            packet,
                            current,
                            contextIds,
                            "presence_events[" + index + "].evidence",
                            findings
                    );
                }
            }
        }

        Object provenance = local.get("provenance");
        if (provenance instanceof Map<?, ?> map && map.get("evidence") instanceof List<?> entries) {
            for (int index = 0; index < entries.size(); index++) {
                if (!matchesEvidence(current.text(), entries.get(index))) {
                    addEvidenceMismatch(
                            revision,
                            packet,
                            current,
                            contextIds,
                            "provenance.evidence[" + index + "]",
                            findings
                    );
                }
            }
        }
    }

    private static void addEvidenceMismatch(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            String path,
            List<DetectorFinding> findings
    ) {
        addFinding(
                revision,
                packet,
                FindingCode.META_TEXT_MISMATCH,
                List.of(current.blockId()),
                List.of(current.scene().id()),
                contextIds,
                Map.of(
                        "evidence_path", path,
                        "kind", "invalid_metadata_evidence",
                        "reason", "stale_or_invalid_span"
                ),
                findings
        );
    }

    private static void inspectPresenceDelta(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            List<DetectorFinding> findings
    ) {
        Set<String> before = strings(current.resolved().before().get("present_character_ids"));
        Set<String> after = strings(current.resolved().after().get("present_character_ids"));
        Set<String> entered = eventCharacters(current.resolved().events(), "enter");
        Set<String> exited = eventCharacters(current.resolved().events(), "exit");

        for (String characterId : difference(after, before)) {
            if (!entered.contains(characterId)) {
                addCharacterFinding(
                        revision,
                        packet,
                        current,
                        contextIds,
                        FindingCode.CHARACTER_APPEARED_WITHOUT_ENTER,
                        characterId,
                        "block_transition",
                        findings
                );
            }
        }
        for (String characterId : difference(before, after)) {
            if (!exited.contains(characterId)) {
                addCharacterFinding(
                        revision,
                        packet,
                        current,
                        contextIds,
                        FindingCode.CHARACTER_DISAPPEARED_WITHOUT_EXIT,
                        characterId,
                        "block_transition",
                        findings
                );
            }
        }
    }

    private static void addCharacterFinding(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            FindingCode code,
            String characterId,
            String kind,
            List<DetectorFinding> findings
    ) {
        addFinding(
                revision,
                packet,
                code,
                List.of(current.blockId()),
                List.of(current.scene().id()),
                contextIds,
                Map.of(
                        "character_id", characterId,
                        "kind", kind,
                        "transition_mode", current.scene().transitionMode().canonicalName()
                ),
                findings
        );
    }

    private static void inspectBoundaries(
            RevisionManifest revision,
            RenderPacket packet,
            List<NarrativeScene> scenes,
            Selection selection,
            List<DetectorFinding> findings
    ) {
        List<DerivedSceneBoundary> boundaries = packet.sceneBoundaries();
        if (boundaries.size() != scenes.size()) {
            throw new IllegalArgumentException("Render packet omitted a scene boundary");
        }
        for (int sceneIndex = 1; sceneIndex < scenes.size(); sceneIndex++) {
            if (!selection.includesBoundary(sceneIndex)) {
                continue;
            }
            NarrativeScene previousScene = scenes.get(sceneIndex - 1);
            NarrativeScene currentScene = scenes.get(sceneIndex);
            DerivedSceneBoundary previous = boundaries.get(sceneIndex - 1);
            DerivedSceneBoundary current = boundaries.get(sceneIndex);
            if (!previous.sceneId().equals(previousScene.id())
                    || !current.sceneId().equals(currentScene.id())) {
                throw new IllegalArgumentException("Render packet scene boundaries are out of order");
            }

            List<Ids.BlockId> boundaryBlocks = boundaryBlockIds(previousScene, currentScene);
            List<Ids.SceneId> boundaryScenes = List.of(previousScene.id(), currentScene.id());
            if (currentScene.transitionMode() == TransitionMode.CONTINUOUS) {
                inspectContinuousBoundary(
                        revision,
                        packet,
                        previous.stateOut(),
                        current.stateIn(),
                        boundaryBlocks,
                        boundaryScenes,
                        findings
                );
            } else {
                inspectResetBoundary(
                        revision,
                        packet,
                        currentScene.transitionMode(),
                        previous.stateOut(),
                        current.stateIn(),
                        boundaryBlocks,
                        boundaryScenes,
                        findings
                );
            }
        }
    }

    private static void inspectContinuousBoundary(
            RevisionManifest revision,
            RenderPacket packet,
            Map<String, Object> stateOut,
            Map<String, Object> stateIn,
            List<Ids.BlockId> blockIds,
            List<Ids.SceneId> sceneIds,
            List<DetectorFinding> findings
    ) {
        addBoundaryFieldFinding(
                revision, packet, "location", FindingCode.LOCATION_CHANGED_WITHOUT_TRANSITION,
                stateOut, stateIn, blockIds, sceneIds, findings
        );
        addBoundaryFieldFinding(
                revision, packet, "weather", FindingCode.WEATHER_CHANGED_WITHOUT_EVIDENCE,
                stateOut, stateIn, blockIds, sceneIds, findings
        );
        addBoundaryFieldFinding(
                revision, packet, "time", FindingCode.TIME_DISCONTINUITY,
                stateOut, stateIn, blockIds, sceneIds, findings
        );
        addBoundaryFieldFinding(
                revision, packet, "pov", FindingCode.POV_CHANGED_WITHOUT_BOUNDARY,
                stateOut, stateIn, blockIds, sceneIds, findings
        );

        Set<String> before = strings(stateOut.get("present_character_ids"));
        Set<String> after = strings(stateIn.get("present_character_ids"));
        for (String characterId : difference(after, before)) {
            addBoundaryCharacterFinding(
                    revision,
                    packet,
                    FindingCode.CHARACTER_APPEARED_WITHOUT_ENTER,
                    characterId,
                    blockIds,
                    sceneIds,
                    findings
            );
        }
        for (String characterId : difference(before, after)) {
            addBoundaryCharacterFinding(
                    revision,
                    packet,
                    FindingCode.CHARACTER_DISAPPEARED_WITHOUT_EXIT,
                    characterId,
                    blockIds,
                    sceneIds,
                    findings
            );
        }
    }

    private static void addBoundaryFieldFinding(
            RevisionManifest revision,
            RenderPacket packet,
            String field,
            FindingCode code,
            Map<String, Object> stateOut,
            Map<String, Object> stateIn,
            List<Ids.BlockId> blockIds,
            List<Ids.SceneId> sceneIds,
            List<DetectorFinding> findings
    ) {
        Object before = stateOut.get(field);
        Object after = stateIn.get(field);
        if (!comparableChange(before, after)) {
            return;
        }
        addFinding(
                revision,
                packet,
                code,
                blockIds,
                sceneIds,
                blockIds,
                Map.of(
                        "after", after,
                        "before", before,
                        "field", field,
                        "kind", "scene_boundary",
                        "transition_mode", TransitionMode.CONTINUOUS.canonicalName()
                ),
                findings
        );
    }

    private static void addBoundaryCharacterFinding(
            RevisionManifest revision,
            RenderPacket packet,
            FindingCode code,
            String characterId,
            List<Ids.BlockId> blockIds,
            List<Ids.SceneId> sceneIds,
            List<DetectorFinding> findings
    ) {
        addFinding(
                revision,
                packet,
                code,
                blockIds,
                sceneIds,
                blockIds,
                Map.of(
                        "character_id", characterId,
                        "kind", "scene_boundary",
                        "transition_mode", TransitionMode.CONTINUOUS.canonicalName()
                ),
                findings
        );
    }

    private static void inspectResetBoundary(
            RevisionManifest revision,
            RenderPacket packet,
            TransitionMode mode,
            Map<String, Object> stateOut,
            Map<String, Object> stateIn,
            List<Ids.BlockId> blockIds,
            List<Ids.SceneId> sceneIds,
            List<DetectorFinding> findings
    ) {
        List<String> changedFields = new ArrayList<>();
        for (String field : OBSERVATION_FIELDS) {
            if (comparableChange(stateOut.get(field), stateIn.get(field))) {
                changedFields.add(field);
            }
        }
        Set<String> before = strings(stateOut.get("present_character_ids"));
        Set<String> after = strings(stateIn.get("present_character_ids"));
        List<String> appeared = difference(after, before);
        List<String> disappeared = difference(before, after);
        if (changedFields.isEmpty() && appeared.isEmpty() && disappeared.isEmpty()) {
            return;
        }

        addFinding(
                revision,
                packet,
                FindingCode.INTENTIONAL_SCENE_RESET,
                blockIds,
                sceneIds,
                blockIds,
                Map.of(
                        "appeared_character_ids", appeared,
                        "changed_fields", List.copyOf(changedFields),
                        "disappeared_character_ids", disappeared,
                        "kind", "scene_boundary",
                        "transition_mode", mode.canonicalName()
                ),
                findings
        );
    }

    private static void addFinding(
            RevisionManifest revision,
            RenderPacket packet,
            FindingCode code,
            List<Ids.BlockId> affectedBlockIds,
            List<Ids.SceneId> affectedSceneIds,
            List<Ids.BlockId> contextBlockIds,
            Map<String, Object> evidence,
            List<DetectorFinding> findings
    ) {
        findings.add(DetectorFinding.create(
                code,
                revision.id(),
                packet.revisionHash(),
                affectedBlockIds,
                affectedSceneIds,
                contextBlockIds,
                evidence
        ));
    }

    private static boolean hasValidEvidence(
            Object observation,
            Map<String, Object> localMetadata,
            String text
    ) {
        if (observation instanceof Map<?, ?> map
                && matchesEvidence(text, map.get("evidence"))) {
            return true;
        }
        Object provenance = localMetadata.get("provenance");
        if (!(provenance instanceof Map<?, ?> map)
                || !(map.get("evidence") instanceof List<?> entries)) {
            return false;
        }
        return entries.stream().anyMatch(entry -> matchesEvidence(text, entry));
    }

    private static boolean matchesEvidence(String text, Object raw) {
        return raw instanceof Map<?, ?> evidence && EvidenceSpans.matches(text, evidence);
    }

    private static boolean isExplicit(Object raw) {
        return raw instanceof Map<?, ?> map && "explicit".equals(map.get("mode"));
    }

    private static boolean comparableChange(Object before, Object after) {
        return comparable(before) && comparable(after) && !Objects.equals(before, after);
    }

    private static boolean comparable(Object value) {
        return !(value instanceof Map<?, ?> map
                && ("unknown".equals(map.get("mode"))
                || "not_applicable".equals(map.get("mode"))));
    }

    private static Set<String> eventCharacters(
            List<Map<String, Object>> events,
            String type
    ) {
        Set<String> characters = new TreeSet<>();
        for (Map<String, Object> event : events) {
            if (type.equals(event.get("type"))
                    && event.get("character_id") instanceof String characterId) {
                characters.add(characterId);
            }
        }
        return characters;
    }

    private static Set<String> strings(Object raw) {
        Set<String> values = new TreeSet<>();
        if (raw instanceof List<?> entries) {
            for (Object entry : entries) {
                if (entry instanceof String value) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static List<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new TreeSet<>(left);
        result.removeAll(right);
        return List.copyOf(result);
    }

    private static List<Ids.BlockId> contextBlockIds(List<BlockContext> blocks, int index) {
        List<Ids.BlockId> result = new ArrayList<>(3);
        if (index > 0) {
            result.add(blocks.get(index - 1).blockId());
        }
        result.add(blocks.get(index).blockId());
        if (index + 1 < blocks.size()) {
            result.add(blocks.get(index + 1).blockId());
        }
        return List.copyOf(result);
    }

    private static List<Ids.BlockId> boundaryBlockIds(
            NarrativeScene previous,
            NarrativeScene current
    ) {
        Set<Ids.BlockId> result = new LinkedHashSet<>();
        if (!previous.blocks().isEmpty()) {
            result.add(previous.blocks().getLast().id());
        }
        if (!current.blocks().isEmpty()) {
            result.add(current.blocks().getFirst().id());
        }
        return List.copyOf(result);
    }

    private static List<NarrativeScene> scenes(RevisionManifest revision) {
        List<NarrativeScene> result = new ArrayList<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            result.addAll(chapter.scenes());
        }
        return List.copyOf(result);
    }

    private static List<BlockContext> blockContexts(
            List<NarrativeScene> scenes,
            RenderPacket packet
    ) {
        List<BlockContext> result = new ArrayList<>();
        int globalIndex = 0;
        for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
            NarrativeScene scene = scenes.get(sceneIndex);
            for (int blockIndex = 0; blockIndex < scene.blocks().size(); blockIndex++) {
                NarrativeBlock block = scene.blocks().get(blockIndex);
                RenderedBlock rendered = packet.blocks().get(globalIndex);
                ResolvedBlockMetadata resolved = packet.resolvedMetadata().get(globalIndex);
                if (!block.id().equals(rendered.blockId())
                        || !block.id().equals(resolved.blockId())) {
                    throw new IllegalArgumentException("Render packet block order does not match revision");
                }
                result.add(new BlockContext(
                        scene,
                        sceneIndex,
                        blockIndex == 0,
                        rendered,
                        resolved
                ));
                globalIndex++;
            }
        }
        if (globalIndex != packet.blocks().size()) {
            throw new IllegalArgumentException("Render packet contains blocks outside the revision");
        }
        return List.copyOf(result);
    }

    private static Selection select(
            List<BlockContext> blocks,
            List<NarrativeScene> scenes,
            RenderRange requestedRange
    ) {
        if (blocks.isEmpty()) {
            if (!requestedRange.isAll()) {
                throw new IllegalArgumentException("An empty revision has no detector endpoints");
            }
            return new Selection(0, -1, 0, scenes.size() - 1, true);
        }
        int from = requestedRange.isAll()
                ? 0 : blockIndex(blocks, requestedRange.fromBlockId());
        int to = requestedRange.isAll()
                ? blocks.size() - 1 : blockIndex(blocks, requestedRange.toBlockId());
        if (from > to) {
            throw new IllegalArgumentException("Detector range endpoints are reversed");
        }
        return new Selection(
                from,
                to,
                blocks.get(from).sceneIndex(),
                blocks.get(to).sceneIndex(),
                requestedRange.isAll()
        );
    }

    private static int blockIndex(List<BlockContext> blocks, Ids.BlockId blockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).blockId().equals(blockId)) {
                return index;
            }
        }
        throw new IllegalArgumentException(
                "Revision does not contain detector endpoint " + blockId.value()
        );
    }

    private record Selection(
            int fromBlock,
            int toBlock,
            int firstScene,
            int lastScene,
            boolean all
    ) {
        boolean includesBoundary(int currentScene) {
            return all || (currentScene >= firstScene && currentScene <= lastScene);
        }
    }

    private record BlockContext(
            NarrativeScene scene,
            int sceneIndex,
            boolean firstInScene,
            RenderedBlock rendered,
            ResolvedBlockMetadata resolved
    ) {
        Ids.BlockId blockId() {
            return rendered.blockId();
        }

        String text() {
            return rendered.text();
        }
    }
}
