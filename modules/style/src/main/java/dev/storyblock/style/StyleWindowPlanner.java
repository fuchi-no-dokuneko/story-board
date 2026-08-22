package dev.storyblock.style;

import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.UnicodeText;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class StyleWindowPlanner {
    public List<StyleWindow> plan(
            RevisionManifest revision,
            StyleWindowConfiguration configuration
    ) {
        Objects.requireNonNull(revision, "revision");
        List<StyleAnalysisBlock> blocks = new ArrayList<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                scene.blocks().forEach(block ->
                        blocks.add(StyleAnalysisBlock.from(scene, block))
                );
            }
        }
        return plan(blocks, configuration);
    }

    public List<StyleWindow> plan(
            List<StyleAnalysisBlock> analysisBlocks,
            StyleWindowConfiguration configuration
    ) {
        analysisBlocks = List.copyOf(analysisBlocks);
        Objects.requireNonNull(configuration, "configuration");
        if (analysisBlocks.isEmpty() || analysisBlocks.size() > 1_000) {
            throw new IllegalArgumentException(
                    "Style window planning requires 1 to 1000 blocks"
            );
        }
        List<Segment> segments = segments(analysisBlocks);
        List<StyleWindow> result = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            Segment segment = segments.get(index);
            result.addAll(windows(
                    segment,
                    index,
                    StyleWindowKind.OPERATIONAL,
                    configuration.operationalGraphemes(),
                    configuration.operationalStrideGraphemes()
            ));
            result.addAll(windows(
                    segment,
                    index,
                    StyleWindowKind.MICRO,
                    configuration.microGraphemes(),
                    configuration.microStrideGraphemes()
            ));
            if (configuration.nonOverlapEnabled()) {
                result.addAll(windows(
                        segment,
                        index,
                        StyleWindowKind.NON_OVERLAP,
                        configuration.operationalGraphemes(),
                        configuration.operationalGraphemes()
                ));
            }
        }
        return List.copyOf(result);
    }

    private static List<Segment> segments(List<StyleAnalysisBlock> blocks) {
        List<Segment> result = new ArrayList<>();
        List<StyleAnalysisBlock> current = new ArrayList<>();
        SegmentKey currentKey = null;
        for (StyleAnalysisBlock block : blocks) {
            SegmentKey key = SegmentKey.from(block);
            if (currentKey != null && !currentKey.equals(key)) {
                result.add(new Segment(currentKey, List.copyOf(current)));
                current.clear();
            }
            currentKey = key;
            current.add(block);
        }
        if (!current.isEmpty()) {
            result.add(new Segment(currentKey, List.copyOf(current)));
        }
        return List.copyOf(result);
    }

    private static List<StyleWindow> windows(
            Segment segment,
            int segmentIndex,
            StyleWindowKind kind,
            int target,
            int stride
    ) {
        List<StyleAnalysisBlock> blocks = segment.blocks();
        int[] prefix = new int[blocks.size() + 1];
        for (int index = 0; index < blocks.size(); index++) {
            prefix[index + 1] = prefix[index]
                    + UnicodeText.graphemeCount(blocks.get(index).block().text());
        }
        List<StyleWindow> result = new ArrayList<>();
        int start = 0;
        while (start < blocks.size()) {
            int end = start;
            while (end < blocks.size() && prefix[end + 1] - prefix[start] < target) {
                end++;
            }
            int exclusiveEnd = Math.min(end + 1, blocks.size());
            int graphemes = prefix[exclusiveEnd] - prefix[start];
            boolean full = graphemes >= target;
            if (full || start == 0) {
                List<StyleAnalysisBlock> members = blocks.subList(start, exclusiveEnd);
                result.add(StyleWindow.create(
                        kind,
                        segmentIndex,
                        stratum(segment.key().stratumKind(), members),
                        segment.key().pov(),
                        segment.key().narrativeMode(),
                        members.stream().map(member -> member.block().id()).toList(),
                        graphemes,
                        full,
                        segment.key().shiftReason()
                ));
            }
            if (!full) {
                break;
            }
            int desired = prefix[start] + stride;
            int next = start + 1;
            while (next < blocks.size() && prefix[next] < desired) {
                next++;
            }
            if (next >= blocks.size()) {
                break;
            }
            start = next;
        }
        return List.copyOf(result);
    }

    private static StyleStratum stratum(
            StyleStratumKind kind,
            List<StyleAnalysisBlock> blocks
    ) {
        if (kind == StyleStratumKind.NARRATION) {
            return StyleStratum.narration();
        }
        Set<String> speakers = new LinkedHashSet<>();
        boolean missingSpeaker = false;
        for (StyleAnalysisBlock block : blocks) {
            if (block.speakerId() == null) {
                missingSpeaker = true;
            } else {
                speakers.add(block.speakerId());
            }
        }
        return !missingSpeaker && speakers.size() == 1
                ? StyleStratum.dialogue(speakers.iterator().next())
                : StyleStratum.dialogue();
    }

    private record Segment(SegmentKey key, List<StyleAnalysisBlock> blocks) {
    }

    private record SegmentKey(
            StyleStratumKind stratumKind,
            String pov,
            String narrativeMode,
            String shiftMarker,
            String shiftReason
    ) {
        static SegmentKey from(StyleAnalysisBlock block) {
            String marker = block.intentionalStyleShiftReason() == null
                    ? null : block.sceneId().value();
            return new SegmentKey(
                    block.stratumKind(),
                    block.pov(),
                    block.narrativeMode(),
                    marker,
                    block.intentionalStyleShiftReason()
            );
        }
    }
}
