package dev.storyblock.domain;

import java.util.Map;

public interface NarrativeText {
    String text();
    BlockMetadata metadata();
    Map<String, Object> extensions();
}
