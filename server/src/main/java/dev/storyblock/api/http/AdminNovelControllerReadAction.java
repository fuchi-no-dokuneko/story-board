package dev.storyblock.api.http;

import dev.storyblock.application.NovelCatalogEntry;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

final class AdminNovelControllerReadAction {
    static ResponseEntity<Map<String, Object>> read(AdminNovelController self, String novelId)  {
        Ids.NovelId id = new Ids.NovelId(novelId);
        NovelCatalogEntry novel = self.catalog.get(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("novel", AdminNovelController.entry(novel));
        response.put("revision", self.catalog.revision(id).envelope());
        response.put("schema_version", AdminNovelController.SCHEMA_VERSION);
        return ResponseEntity.ok().eTag(novel.headHash()).body(response);
    }
}
