package dev.storyblock.worker.style;

final class StyleWorkerProtocolException extends RuntimeException {
    StyleWorkerProtocolException(String message) {
        super(message);
    }

    StyleWorkerProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
