package dev.storyblock.renderer;

import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.UnicodeText;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

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
