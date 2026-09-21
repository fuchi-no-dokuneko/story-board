package dev.storyblock.cli;

import dev.storyblock.domain.*;
import dev.storyblock.storage.RevisionStore;
import java.util.Base64;

final class HtmlBook {
    static String render(RevisionManifest revision, RevisionStore store) {
        String title = String.valueOf(revision.novel().extensions().getOrDefault("title", "StoryBlock"));
        var html = new StringBuilder("<!doctype html><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"><title>")
                .append(escape(title)).append("</title><style>body{max-width:48rem;margin:3rem auto;padding:1rem;font:20px/1.8 serif}p{white-space:pre-wrap}img{max-width:100%;height:auto}</style><main><h1>")
                .append(escape(title)).append("</h1>");
        for (var chapter : revision.novel().chapters()) {
            html.append("<section><h2>").append(escape(chapter.title())).append("</h2>");
            for (var scene : chapter.scenes()) {
                if (scene.title() != null && !scene.title().equals(chapter.title()))
                    html.append("<h3>").append(escape(scene.title())).append("</h3>");
                for (var block : scene.blocks()) {
                    block.image().ifPresent(image -> {
                        var artifact = store.getArtifact(image.artifactId());
                        if (!artifact.novelId().equals(revision.novel().id())
                                || !artifact.contentHash().equals(image.contentHash()))
                            throw new IllegalArgumentException("Image artifact mismatch");
                        html.append("<figure><img alt=\"").append(escape(image.altText()))
                                .append("\" src=\"data:").append(image.mediaType()).append(";base64,")
                                .append(Base64.getEncoder().encodeToString(artifact.content())).append("\"><figcaption>")
                                .append(escape(block.text())).append("</figcaption></figure>");
                    });
                    if (block.image().isEmpty()) html.append("<p>").append(escape(block.text())).append("</p>");
                }
            }
            html.append("</section>");
        }
        return html.append("</main>").toString();
    }

    static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
