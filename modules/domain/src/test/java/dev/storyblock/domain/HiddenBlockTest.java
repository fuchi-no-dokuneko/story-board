package dev.storyblock.domain;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenBlockTest {
    private BlockDraft draft(String text, Map<String,Object> meta, Object flag) {
        return new BlockDraft(Ids.BlockId.create(), text, new BlockMetadata(meta),
                Map.of(BlockHidden.EXTENSION_KEY, flag));
    }

    @Test void notesNormalizeAndAllowLongMultilineExplanations() {
        var draft = draft("  Cafe\u0301\n" + "Foreshadowing. Motive. Payoff. ".repeat(20), Map.of(), true);
        assertTrue(draft.isHidden());
        assertTrue(draft.text().startsWith("Café\n"));
        assertFalse(draft.text().endsWith(" "));
        assertTrue(draft.materialize(OrderKey.initial()).isHidden());
        assertEquals("意".repeat(2000), HiddenText.validate("意".repeat(2000)));
        assertEquals("👨‍👩‍👧‍👦".repeat(2000), HiddenText.validate("👨‍👩‍👧‍👦".repeat(2000)));
        for (String invalid : new String[]{" \n ", "意".repeat(2001)}) {
            assertEquals(HiddenBlockException.Code.HIDDEN_BLOCK_TEXT_INVALID,
                    assertThrows(HiddenBlockException.class, () -> draft(invalid, Map.of(), true)).code());
        }
        assertThrows(InvalidBlockTextException.class, () -> NarrativeBlock.create(
                Ids.BlockId.create(), OrderKey.initial(), draft.text(), BlockMetadata.empty(), Map.of()));
    }

    @Test void flagsImagesAndStateMetadataAreStrictlySeparated() {
        for (Object flag : new Object[]{false, "true", 1}) {
            assertEquals(HiddenBlockException.Code.HIDDEN_BLOCK_UNSUPPORTED,
                    assertThrows(HiddenBlockException.class, () -> draft("Motive", Map.of(), flag)).code());
        }
        for (String field : new String[]{"time","location","weather","speech","actions",
                "presence_events","pov","narrative_mode"}) {
            assertEquals(HiddenBlockException.Code.HIDDEN_BLOCK_META_NOT_ALLOWED,
                    assertThrows(HiddenBlockException.class,
                            () -> draft("Motive", Map.of(field, java.util.List.of()), true)).code());
        }
        assertTrue(draft("Motive", Map.of("provenance", Map.of("author", "Lin")), true).isHidden());
        assertEquals(HiddenBlockException.Code.HIDDEN_BLOCK_UNSUPPORTED,
                assertThrows(HiddenBlockException.class, () -> new BlockDraft(Ids.BlockId.create(),
                        "Motive", BlockMetadata.empty(), Map.of(BlockHidden.EXTENSION_KEY, true,
                        BlockImage.EXTENSION_KEY, Map.of()))).code());
        assertFalse(BlockDraft.create("她微笑了。", BlockMetadata.empty()).isHidden());
    }
}
