package dev.storyblock.worker.llm;



final class HttpLlmModelTransportContains {
    static boolean contains(
            byte[] value,
            byte[] candidate,
            int[] prefix
    ) {
        if (candidate.length == 0 || candidate.length > value.length) {
            return false;
        }
        int matched = 0;
        for (byte current : value) {
            while (matched > 0 && current != candidate[matched]) {
                matched = prefix[matched - 1];
            }
            if (current == candidate[matched]) {
                matched++;
            }
            if (matched == candidate.length) {
                return true;
            }
        }
        return false;
    }
}
