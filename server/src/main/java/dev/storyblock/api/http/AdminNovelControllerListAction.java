package dev.storyblock.api.http;

import dev.storyblock.application.NovelCatalogEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;

final class AdminNovelControllerListAction {
    static ResponseEntity<Map<String, Object>> list(AdminNovelController self, int page, int size, String q)  {
        if (page < 0) {
            throw new IllegalArgumentException("page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (q.codePointCount(0, q.length()) > 200) {
            throw new IllegalArgumentException("q cannot exceed 200 Unicode characters");
        }
        List<NovelCatalogEntry> matching = self.catalog.list(q);
        int from = Math.min(Math.multiplyExact(page, size), matching.size());
        int to = Math.min(from + size, matching.size());
        int totalPages = matching.isEmpty() ? 0 : (matching.size() + size - 1) / size;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", matching.subList(from, to).stream()
                .map(AdminNovelController::entry)
                .toList());
        response.put("page", page);
        response.put("schema_version", AdminNovelController.SCHEMA_VERSION);
        response.put("size", size);
        response.put("total", matching.size());
        response.put("total_pages", totalPages);
        return ResponseEntity.ok(response);
    }
}
