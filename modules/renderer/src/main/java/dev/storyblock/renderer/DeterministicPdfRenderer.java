package dev.storyblock.renderer;

import dev.storyblock.domain.RevisionManifest;
import java.util.Set;
import java.util.regex.Pattern;

/** Deterministically lays out a revision and packages rasterized A4 pages as PDF. */
public final class DeterministicPdfRenderer {
  public static final String VERSION = "pdf-renderer-1.0.0";

  static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");
  static final int PAGE_WIDTH = 794;
  static final int PAGE_HEIGHT = 1_123;
  static final int MARGIN_X = 72;
  static final int MARGIN_TOP = 72;
  static final int MARGIN_BOTTOM = 70;
  static final int BODY_SIZE = 20;
  static final int BODY_LEADING = 32;
  static final int MAX_IMAGE_HEIGHT = 500;
  static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
      "image/jpeg", "image/png"
  );

  public PdfRenderResult render(
      RevisionManifest revision,
      String revisionHash,
      PdfImageResolver imageResolver
  ) {
    return DeterministicPdfRendererRenderAction.render(this, revision, revisionHash, imageResolver);
  }

}
