package dev.storyblock.worker.style;



final class StyleWorkerClientUnquoteEtag {
    static String unquoteEtag(String value) {
        if (value.length() < 2 || value.charAt(0) != '"'
                || value.charAt(value.length() - 1) != '"') {
            throw new StyleWorkerProtocolException(
                    "Style job claim ETag is not a quoted strong ETag"
            );
        }
        return value.substring(1, value.length() - 1);
    }
}
