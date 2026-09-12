package dev.storyblock.worker.llm;

final class LlmWorkerProtocolException extends RuntimeException {
    LlmWorkerProtocolException(String message) {
        super(message);
    }
}
