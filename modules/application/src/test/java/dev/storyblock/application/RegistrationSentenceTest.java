package dev.storyblock.application;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegistrationSentenceTest {
    @Test void closingQuotesStayWithTheSentenceBeforeTheNextBlock() {
        String text = "他等了一夜。「先別拆。」他說。「這封信還沒有寫完。」她點頭。";
        List<String> blocks = AgentNovelRegistrationServiceBlocks.blocks(text, 0);
        assertEquals(List.of("他等了一夜。「先別拆。」", "他說。「這封信還沒有寫完。」", "她點頭。"), blocks);
        assertEquals(text, String.join("", blocks));
        assertTrue(blocks.stream().noneMatch(block -> block.startsWith("」")));
    }
}
