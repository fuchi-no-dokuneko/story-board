package dev.storyblock.application;

import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import java.util.ArrayList;
import java.util.List;

final class NarrativeEditorReplaceScene {
    static NarrativeNovel replaceScene(NarrativeNovel novel, NarrativeScene replacement) {
        List<NarrativeChapter> chapters = new ArrayList<>(novel.chapters());
        for (int chapterIndex = 0; chapterIndex < chapters.size(); chapterIndex++) {
            NarrativeChapter chapter = chapters.get(chapterIndex);
            List<NarrativeScene> scenes = new ArrayList<>(chapter.scenes());
            for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
                if (scenes.get(sceneIndex).id().equals(replacement.id())) {
                    scenes.set(sceneIndex, replacement);
                    chapters.set(chapterIndex, chapter.withScenes(scenes));
                    return novel.withChapters(chapters);
                }
            }
        }
        throw NarrativeEditorInvalid.invalid("Replacement scene is not part of the novel: " + replacement.id().value());
    }
}
