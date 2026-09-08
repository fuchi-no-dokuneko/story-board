package dev.storyblock.application;

import dev.storyblock.style.StyleFeatureChannel;

final class RewriteGateServiceDirective {
    static String directive(StyleFeatureChannel channel) {
        return "Reduce " + channel.canonicalName()
                + " style deviation while preserving facts and metadata.";
    }
}
