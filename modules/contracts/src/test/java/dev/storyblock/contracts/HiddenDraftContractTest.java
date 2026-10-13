package dev.storyblock.contracts;

import dev.storyblock.domain.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HiddenDraftContractTest {
    @Test void hiddenDraftUsesDedicatedWireFieldAndRoundTrips() {
        var wire = Map.<String,Object>of("id", Ids.BlockId.create().value(), "text",
                "Foreshadowing\n" + "The teacup reflects a concealed motive. ".repeat(10),
                "meta", Map.of("provenance", Map.of("author", "Lin")), "hidden", true,
                "extensions", Map.of("vendor.note", "retained"));
        var draft = EditOperationCanonicalMapperParseDraft.parseDraft(wire);
        var encoded = EditOperationCanonicalMapperDraft.draft(draft);
        assertEquals(true, encoded.get("hidden"));
        assertEquals(Map.of("vendor.note", "retained"), encoded.get("extensions"));
        assertEquals(draft, EditOperationCanonicalMapperParseDraft.parseDraft(encoded));
        assertThrows(HiddenBlockException.class, () -> EditOperationCanonicalMapperParseDraft.parseDraft(
                Map.of("id", draft.id().value(), "text", draft.text(), "meta", Map.of(),
                        "extensions", Map.of(BlockHidden.EXTENSION_KEY, true))));
        assertThrows(HiddenBlockException.class, () -> EditOperationCanonicalMapperParseDraft.parseDraft(
                Map.of("id", draft.id().value(), "text", draft.text(), "meta", Map.of(), "hidden", false)));
    }

    @Test void canonicalBlockAcceptsNoteTextButRejectsStateAndImage() {
        var block = new java.util.HashMap<String,Object>(Map.of("id", Ids.BlockId.create().value(),
                "block_version_id", Ids.BlockVersionId.create().value(), "order_key", OrderKey.initial().value(),
                "text", "Plot motive\n" + "意".repeat(300), "meta", Map.of(),
                "extensions", Map.of(BlockHidden.EXTENSION_KEY, true)));
        assertDoesNotThrow(() -> CanonicalRevisionValidateBlock.validateBlock(block, "scene", 0));
        block.put("meta", Map.of("weather", "rain"));
        assertEquals(HiddenBlockException.Code.HIDDEN_BLOCK_META_NOT_ALLOWED,
                assertThrows(HiddenBlockException.class,
                        () -> CanonicalRevisionValidateBlock.validateBlock(block,"scene",0)).code());
        block.put("meta", Map.of()); block.put("extensions", Map.of());
        assertThrows(InvalidBlockTextException.class,
                () -> CanonicalRevisionValidateBlock.validateBlock(block,"scene",0));
    }
}
