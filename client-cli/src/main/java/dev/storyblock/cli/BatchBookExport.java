package dev.storyblock.cli;

import dev.storyblock.application.PdfRenderService;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.io.PrintStream;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class BatchBookExport {
    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length < 4 || !Set.of("html", "pdf", "both").contains(args[3])) return 2;
        Path database = Path.of(args[1]), directory = Path.of(args[2]);
        if (!Files.isRegularFile(database)) { err.println("Database missing / 資料庫不存在"); return 1; }
        try (var store = SqliteRevisionStore.open(database)) {
            var ids = args.length == 4 ? store.listNovels() : Arrays.stream(args).skip(4)
                    .map(Ids.NovelId::new).distinct().toList();
            var revisions = ids.stream().map(id -> store.getRevision(id, store.getHead(id).revisionId())).toList();
            Files.createDirectories(directory);
            var index = new StringBuilder("<!doctype html><meta charset=\"utf-8\"><title>StoryBlock</title><h1>Books / 小說</h1><ul>");
            for (var revision : revisions) {
                var manifest = revision.manifest();
                String id = manifest.novel().id().value();
                String title = String.valueOf(manifest.novel().extensions().getOrDefault("title", id));
                index.append("<li>").append(HtmlBook.escape(title));
                for (String format : args[3].equals("both") ? List.of("html", "pdf") : List.of(args[3])) {
                    byte[] bytes = format.equals("html") ? HtmlBook.render(manifest, store).getBytes(StandardCharsets.UTF_8)
                            : new PdfRenderService(store).render(manifest.novel().id(), manifest.id(), revision.contentHash()).content();
                    write(directory.resolve(id + "." + format), bytes);
                    index.append(" <a href=\"").append(id).append('.').append(format).append("\">").append(format).append("</a>");
                }
                index.append("</li>");
                out.println(id + " " + title);
            }
            write(directory.resolve("index.html"), index.append("</ul>").toString().getBytes(StandardCharsets.UTF_8));
            return 0;
        } catch (Exception error) { err.println(error.getMessage()); return 1; }
    }

    private static void write(Path target, byte[] bytes) throws Exception {
        Path pending = Files.createTempFile(target.getParent(), ".export-", ".pending");
        try { Files.write(pending, bytes); Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        finally { Files.deleteIfExists(pending); }
    }
}
