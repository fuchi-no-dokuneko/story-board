package dev.storyblock.renderer;
import dev.storyblock.domain.DerivedSceneBoundary;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.renderer.DeterministicRenderer.ResolvedEntry;
import static dev.storyblock.renderer.DeterministicRenderer.Resolution;

final class RenderedRange {
  static RenderPacket create(RevisionManifest revision, String revisionHash, RenderRange requestedRange, Resolution resolution, List<ResolvedEntry> resolved, int from, int to) {
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
    );  }
}
