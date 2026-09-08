package dev.storyblock.renderer;

import dev.storyblock.domain.DerivedSceneBoundary;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class DeterministicRenderer {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

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

        Resolution resolution = DeterministicRendererResolveAll.resolveAll(revision);
        List<ResolvedEntry> resolved = resolution.entries();
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
                    List.of(),
                    resolution.sceneBoundaries()
            );
        }

        int from = requestedRange.isAll() ? 0 : DeterministicRendererIndexOf.indexOf(resolved, requestedRange.fromBlockId());
        int to = requestedRange.isAll()
                ? resolved.size() - 1
                : DeterministicRendererIndexOf.indexOf(resolved, requestedRange.toBlockId());
        if (from > to) {
            throw new IllegalArgumentException("Render range endpoints are reversed");
        }

        List<RenderedBlock> blocks = new ArrayList<>();
        List<ResolvedBlockMetadata> metadata = new ArrayList<>();
        List<OffsetMapEntry> offsets = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int renderedCodePoints = 0;
        for (int index = from; index <= to; index++) {
            ResolvedEntry entry = resolved.get(index);
            if (!blocks.isEmpty()) {
                text.append('\n');
                renderedCodePoints++;
            }
            int start = renderedCodePoints;
            text.append(entry.block().text());
            renderedCodePoints += entry.block().text().codePointCount(
                    0, entry.block().text().length()
            );
            int end = renderedCodePoints;
            blocks.add(new RenderedBlock(
                    entry.block().id(),
                    entry.block().versionId(),
                    entry.block().text(),
                    entry.block().metadata(),
                    entry.block().image().orElse(null)
            ));
            metadata.add(entry.resolved());
            offsets.add(new OffsetMapEntry(entry.block().id(), start, end));
        }

        RenderRange actualRange = RenderRange.inclusive(
                blocks.getFirst().blockId(), blocks.getLast().blockId()
        );
        List<DerivedSceneBoundary> sceneBoundaries = requestedRange.isAll()
                ? resolution.sceneBoundaries()
                : resolution.sceneBoundaries().subList(
                        resolved.get(from).sceneIndex(),
                        resolved.get(to).sceneIndex() + 1
                );
        return new RenderPacket(
                revision.novel().id(),
                revision.id(),
                revisionHash,
                RendererModule.VERSION,
                actualRange,
                text.toString(),
                blocks,
                metadata,
                offsets,
                sceneBoundaries
        );
    }

    record Resolution(
            List<ResolvedEntry> entries,
            List<DerivedSceneBoundary> sceneBoundaries
    ) {
    }

    record ResolvedEntry(
            int sceneIndex,
            NarrativeBlock block,
            ResolvedBlockMetadata resolved
    ) {
    }
}
