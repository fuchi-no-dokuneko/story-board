package dev.storyblock.contracts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalRevisionTest {
    @Test
    void goldenCanonicalBytesAndHashAreRepeatable() throws IOException {
        CanonicalRevision first = CanonicalRevision.of(baseContent());
        CanonicalRevision second = CanonicalRevision.of(baseContent());

        assertArrayEquals(readFixture("canonical-revision-content.json"), first.canonicalBytes());
        assertArrayEquals(first.canonicalBytes(), second.canonicalBytes());
        assertEquals(readFixtureText("canonical-revision-content.sha256"), first.contentHash());
        assertEquals(first.contentHash(), second.contentHash());

        CanonicalRevision parsed = CanonicalRevision.parseEnvelope(first.envelopeBytes());
        assertArrayEquals(first.canonicalBytes(), parsed.canonicalBytes());
    }

    @Test
    void derivedRenderAndCacheDataNeverAffectCanonicalBytesOrHash() {
        CanonicalRevision first = CanonicalRevision.of(
                baseContent(),
                Map.of("render", Map.of("text", "first"), "cache", Map.of("hit", true))
        );
        CanonicalRevision second = CanonicalRevision.of(
                baseContent(),
                Map.of("render", Map.of("text", "second"), "cache", Map.of("hit", false))
        );

        assertArrayEquals(first.canonicalBytes(), second.canonicalBytes());
        assertEquals(first.contentHash(), second.contentHash());
        assertNotEquals(
                new String(first.diagnosticExportBytes(), StandardCharsets.UTF_8),
                new String(second.diagnosticExportBytes(), StandardCharsets.UTF_8)
        );
    }

    @Test
    void unknownRootAndNestedFieldsAreRejected() {
        Map<String, Object> unknownRoot = baseContent();
        unknownRoot.put("render_cache", Map.of());

        Map<String, Object> unknownChapter = baseContent();
        @SuppressWarnings("unchecked")
        Map<String, Object> chapter = (Map<String, Object>) ((List<?>) unknownChapter.get("chapters")).getFirst();
        chapter.put("unsafe", true);

        assertThrows(IllegalArgumentException.class, () -> CanonicalRevision.of(unknownRoot));
        assertThrows(IllegalArgumentException.class, () -> CanonicalRevision.of(unknownChapter));
    }

    @Test
    void envelopeWithIncorrectDeclaredHashIsRejected() {
        CanonicalRevision revision = CanonicalRevision.of(baseContent());
        String envelope = new String(revision.envelopeBytes(), StandardCharsets.UTF_8)
                .replace(revision.contentHash(), "sha256:" + "0".repeat(64));

        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalRevision.parseEnvelope(envelope.getBytes(StandardCharsets.UTF_8))
        );
    }

    private static Map<String, Object> baseContent() {
        Map<String, Object> chapter = new LinkedHashMap<>();
        chapter.put("title", "Arrival");
        chapter.put("scenes", new ArrayList<>());
        chapter.put("id", "ch_018f0f5e-7b4a-7c00-8000-000000000002");
        chapter.put("order_key", "55555555555555555555555555555555");

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("revision_id", "rev_018f0f5e-7b4a-7c00-8000-000000000003");
        content.put("created_at", "2026-08-20T12:00:00Z");
        content.put("chapters", new ArrayList<>(List.of(chapter)));
        content.put("schema_version", CanonicalRevision.SCHEMA_VERSION);
        content.put("parent_revision_id", null);
        content.put("novel_id", "nov_018f0f5e-7b4a-7c00-8000-000000000001");
        return content;
    }

    private static byte[] readFixture(String name) throws IOException {
        return readFixtureText(name).getBytes(StandardCharsets.UTF_8);
    }

    private static String readFixtureText(String name) throws IOException {
        try (InputStream input = CanonicalRevisionTest.class.getResourceAsStream("/golden/" + name)) {
            if (input == null) {
                throw new IOException("Missing golden fixture " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).stripTrailing();
        }
    }
}
