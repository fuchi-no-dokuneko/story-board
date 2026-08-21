package dev.storyblock.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ReplayVerificationReport(List<ReplayVerification> novels) {
    public ReplayVerificationReport {
        novels = List.copyOf(novels);
    }

    public boolean valid() {
        return novels.stream().allMatch(ReplayVerification::valid);
    }

    public Map<String, Object> contractFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("valid", valid());
        fields.put("novel_count", novels.size());
        fields.put("novels", novels.stream().map(ReplayVerification::contractFields).toList());
        return java.util.Collections.unmodifiableMap(fields);
    }
}
