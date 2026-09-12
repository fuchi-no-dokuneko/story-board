package dev.storyblock.renderer;

import dev.storyblock.domain.RevisionManifest;
import java.util.List;
import java.util.Objects;
import static dev.storyblock.renderer.DeterministicRenderer.ResolvedEntry;
import static dev.storyblock.renderer.DeterministicRenderer.Resolution;

final class DeterministicRendererRenderAction {
  static RenderPacket render(DeterministicRenderer self, RevisionManifest revision, String revisionHash, RenderRange requestedRange)  {
    Objects.requireNonNull(revision, "revision");
    if (revisionHash == null || !DeterministicRenderer.SHA_256.matcher(revisionHash).matches()) {
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

    return RenderedRange.create(revision, revisionHash, requestedRange, resolution, resolved, from, to);
  }
}
