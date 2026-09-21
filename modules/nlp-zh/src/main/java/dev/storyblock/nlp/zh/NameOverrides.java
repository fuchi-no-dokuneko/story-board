package dev.storyblock.nlp.zh;

import java.util.*;

/** Owned by one analysis; never updates the shared dictionary. */
final class NameOverrides {
    private final Map<Integer, NameOverrides> children = new HashMap<>();
    private boolean terminal;
    NameOverrides(Set<String> names) {
        for (String name : names) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Proper name must be nonblank");
            var node = this;
            for (int cp : name.codePoints().toArray()) node = node.children.computeIfAbsent(cp, k -> new NameOverrides());
            node.terminal = true;
        }
    }
    private NameOverrides() { }
    int[] matches(int[] text) {
        int[] ends = new int[text.length];
        for (int i = 0; i < text.length; i++) {
            var node = this;
            for (int j = i; j < text.length; j++) {
                node = node.children.get(text[j]);
                if (node == null) break;
                if (node.terminal) ends[i] = j + 1;
            }
        }
        return ends;
    }
}
