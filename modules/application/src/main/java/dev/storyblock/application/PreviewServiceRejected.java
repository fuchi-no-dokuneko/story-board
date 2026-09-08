package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.validator.ValidationReport;
import java.util.LinkedHashMap;
import java.util.Map;

final class PreviewServiceRejected {
    static PreviewResponse rejected(
            RevisionManifest base,
            String baseHash,
            Map<String, Object> normalizedOperation,
            ValidationReport report
    ) {
        Map<String, Object> fingerprint = new LinkedHashMap<>();
        fingerprint.put("base_hash", baseHash);
        fingerprint.put("normalized_operation", normalizedOperation);
        return new PreviewResponse(
                base.id(),
                baseHash,
                normalizedOperation,
                CanonicalJson.hash(fingerprint),
                RevisionDiff.empty(),
                null,
                report.violations(),
                report.warnings(),
                false
        );
    }
}
