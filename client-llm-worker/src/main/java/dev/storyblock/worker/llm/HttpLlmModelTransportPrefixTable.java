package dev.storyblock.worker.llm;



final class HttpLlmModelTransportPrefixTable {
    static int[] prefixTable(byte[] candidate) {
        int[] prefix = new int[candidate.length];
        int matched = 0;
        for (int index = 1; index < candidate.length; index++) {
            while (matched > 0 && candidate[index] != candidate[matched]) {
                matched = prefix[matched - 1];
            }
            if (candidate[index] == candidate[matched]) {
                matched++;
            }
            prefix[index] = matched;
        }
        return prefix;
    }
}
