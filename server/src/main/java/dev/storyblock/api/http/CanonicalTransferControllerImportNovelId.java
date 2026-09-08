package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.domain.Ids;
import java.util.Map;

final class CanonicalTransferControllerImportNovelId {
    static Ids.NovelId importNovelId(
            CanonicalExportFormat format,
            Map<String, Object> document
    ) {
        String value = switch (format) {
            case REVISION -> CanonicalTransferControllerString.string(document, "novel_id", "import request.document");
            case PACKAGE -> CanonicalTransferControllerString.string(
                    CanonicalTransferController.object(document.get("manifest"), "import request.document.manifest"),
                    "novel_id",
                    "import request.document.manifest"
            );
        };
        return new Ids.NovelId(value);
    }
}
