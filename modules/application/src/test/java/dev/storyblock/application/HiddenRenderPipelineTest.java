package dev.storyblock.application;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.*;
import dev.storyblock.renderer.*;
import dev.storyblock.detector.AdjacentMetadataDetector;
import dev.storyblock.monitor.MonitorPacketFactory;
import dev.storyblock.validator.DeterministicValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenRenderPipelineTest {
    @Test void proseOffsetsStatePdfDetectorAndMonitorEqualNotesRemoved() {
        var full = HiddenBlockFixture.revision();
        var plain = HiddenBlockFixture.withoutHidden(full);
        var renderer = new DeterministicRenderer(); var range = RenderRange.all();
        var actual = renderer.render(full,HiddenBlockFixture.HASH,range);
        assertEquals(renderer.render(plain,HiddenBlockFixture.HASH,range),actual);
        assertFalse(actual.renderedText().contains(HiddenBlockFixture.SENTINEL));
        assertEquals(4,actual.blocks().size());
        assertEquals("renderer-1.1.0",actual.rendererVersion());
        var pdf = new DeterministicPdfRenderer();
        assertArrayEquals(pdf.render(plain,HiddenBlockFixture.HASH, ignored -> null).content(),
                pdf.render(full,HiddenBlockFixture.HASH, ignored -> null).content());
        var detector = new AdjacentMetadataDetector();
        assertEquals(detector.detect(plain,HiddenBlockFixture.HASH,range),
                detector.detect(full,HiddenBlockFixture.HASH,range));
        var target = NarrativeView.visibleBlocks(full).get(1).id();
        var factory = new MonitorPacketFactory();
        assertEquals(factory.create(plain,HiddenBlockFixture.HASH,target,2),
                factory.create(full,HiddenBlockFixture.HASH,target,2));
        assertTrue(new DeterministicValidator().validateRevision(full,full,HiddenBlockFixture.HASH).committable());
        var note = full.liveBlocks().getFirst();
        assertEquals(HiddenBlockException.Code.HIDDEN_BLOCK_NOT_RENDERABLE,
                assertThrows(HiddenBlockException.class, () -> renderer.render(full,HiddenBlockFixture.HASH,
                        RenderRange.inclusive(note.id(),target))).code());
    }

    @Test void canonicalProjectionRetainsEveryNoteAndIdentity() {
        var revision = HiddenBlockFixture.revision();
        var canonical = NarrativeCanonicalMapper.toCanonical(revision);
        assertTrue(new String(canonical.canonicalBytes(),java.nio.charset.StandardCharsets.UTF_8)
                .contains(HiddenBlockFixture.SENTINEL));
        assertEquals(revision,NarrativeCanonicalMapper.fromCanonical(
                CanonicalRevision.parseEnvelope(canonical.envelopeBytes())));
        assertEquals(10,revision.selectedBlockVersions().size());
    }
}
