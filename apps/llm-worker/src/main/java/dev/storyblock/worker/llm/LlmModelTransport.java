package dev.storyblock.worker.llm;

import java.io.IOException;

@FunctionalInterface
interface LlmModelTransport {
    byte[] invoke(byte[] canonicalRequest) throws IOException, InterruptedException;
}
