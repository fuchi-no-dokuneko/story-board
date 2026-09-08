package dev.storyblock.renderer;

import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;

final class DeterministicPdfRendererVisibleText {
    static String visibleText(RevisionManifest revision, String title) {
        StringBuilder text = new StringBuilder(title);
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            if (chapter.title() != null) {
                text.append(chapter.title());
            }
            for (NarrativeScene scene : chapter.scenes()) {
                if (scene.title() != null) {
                    text.append(scene.title());
                }
                scene.blocks().forEach(block -> text.append(block.text()));
            }
        }
        return text.toString();
    }
}
