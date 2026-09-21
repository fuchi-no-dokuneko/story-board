package dev.storyblock.nlp.zh;

import java.util.function.BiConsumer;

/** Trie DAG + backward dynamic programming, adapted from Jieba SEARCH (Apache-2.0). */
final class DagSegmenter {
    private final DictionarySnapshot dictionary;
    private final HmmSegmenter hmm;
    DagSegmenter(DictionarySnapshot dictionary, HmmSegmenter hmm) { this.dictionary = dictionary; this.hmm = hmm; }
    void cut(int[] text, int start, int end, BiConsumer<Integer, Integer> output) {
        int count = end - start;
        double[] scores = new double[count + 1];
        int[] route = new int[count];
        for (int at = end - 1; at >= start; at--) {
            double best = Double.NEGATIVE_INFINITY;
            int chosen = at + 1, node = 0;
            for (int next = at; next < end; next++) {
                node = dictionary.child(node, text[next]);
                if (node == 0) break;
                double value = dictionary.score(node) + scores[next + 1 - start];
                if (value > best) { best = value; chosen = next + 1; }
            }
            if (best == Double.NEGATIVE_INFINITY) best = dictionary.unknown + scores[at + 1 - start];
            scores[at - start] = best; route[at - start] = chosen;
        }
        int at = start;
        while (at < end) {
            int next = route[at - start];
            if (next > at + 1) { output.accept(at, next); at = next; continue; }
            int last = next;
            while (last < end && route[last - start] == last + 1) last++;
            if (last == at + 1 || known(text, at, last)) output.accept(at, last);
            else hmm.cut(text, at, last, output);
            at = last;
        }
    }
    private boolean known(int[] text, int begin, int end) {
        int node = 0;
        for (int i = begin; i < end; i++) { node = dictionary.child(node, text[i]); if (node == 0) return false; }
        return dictionary.score(node) != Double.NEGATIVE_INFINITY;
    }
}
