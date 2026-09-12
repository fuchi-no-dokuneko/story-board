package dev.storyblock.api.http;



final class CanonicalTransferControllerQuotedEtag {
    static String quotedEtag(String hash) {
        return "\"" + hash + "\"";
    }
}
