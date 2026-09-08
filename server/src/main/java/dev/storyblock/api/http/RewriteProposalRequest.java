package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisJob;
import java.util.*;

final class RewriteProposalRequest {
    private static final Set<String> REQUEST_FIELDS = Set.of(
    "analysis_id", "novel_id", "revision_id", "revision_hash",
    "finding_ids", "cooldown_seconds"
    );

    static void requireRequestBinding(
    Map<String, Object> request,
    StyleAnalysisJob analysis,
    Ids.NovelId novelId
    ) {
    if (!analysis.snapshot().novelId().equals(novelId)
    || !analysis.snapshot().revisionId().value().equals(
    StrictJsonRequest.string(
        request, "revision_id", "rewrite proposal request"
    )
    )
    || !analysis.snapshot().revisionHash().equals(
    StrictJsonRequest.string(
        request, "revision_hash", "rewrite proposal request"
    )
    )) {
    throw new IllegalArgumentException(
        "Rewrite proposal request does not match its style analysis"
    );
    }
    }

    static void requireRequestFields(Map<String, Object> request) {
    for (String required : Set.of(
    "analysis_id", "novel_id", "revision_id", "revision_hash",
    "finding_ids"
    )) {
    if (!request.containsKey(required)) {
    throw new IllegalArgumentException(
    "rewrite proposal request is missing " + required
    );
    }
    }
    for (String field : request.keySet()) {
    if (!REQUEST_FIELDS.contains(field)) {
    throw new IllegalArgumentException(
    "rewrite proposal request contains unknown field " + field
    );
    }
    }
    }

    static List<String> strings(Object value) {
    if (!(value instanceof List<?> entries)) {
    throw new IllegalArgumentException(
        "rewrite proposal request.finding_ids must be an array"
    );
    }
    return entries.stream().map(entry -> {
    if (!(entry instanceof String text)) {
    throw new IllegalArgumentException(
    "rewrite proposal request.finding_ids must contain strings"
    );
    }
    return text;
    }).toList();
    }
}
