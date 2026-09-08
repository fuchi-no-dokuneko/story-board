package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleFeatureAnalyzerDistance {
    static StyleChannelDistance distance(
            StyleFeatureChannel channel,
            StyleFeatureVector target,
            StyleFeatureVector current,
            BigDecimal alpha
    ) {
        BigDecimal primary;
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        switch (channel.primaryMetric()) {
            case JENSEN_SHANNON_DISTANCE -> {
                primary = StyleFeatureAnalyzerDecimal.decimal(StyleFeatureAnalyzerJsDistance.jsDistance(
                        target.distribution(), current.distribution(), alpha.doubleValue()
                ));
                if (channel == StyleFeatureChannel.SURFACE) {
                    diagnostics.put("kl_current_target", StyleFeatureAnalyzerDecimal.decimal(StyleFeatureAnalyzerKl.kl(
                            current.distribution(), target.distribution(), alpha.doubleValue()
                    )));
                    diagnostics.put("kl_target_current", StyleFeatureAnalyzerDecimal.decimal(StyleFeatureAnalyzerKl.kl(
                            target.distribution(), current.distribution(), alpha.doubleValue()
                    )));
                    diagnostics.put("token_kl_diagnostic_only", true);
                }
            }
            case WASSERSTEIN_DISTANCE -> {
                primary = StyleFeatureAnalyzerDecimal.decimal(StyleFeatureAnalyzerWasserstein.wasserstein(
                        target.distribution(), current.distribution()
                ));
                diagnostics.put("jensen_shannon_secondary", StyleFeatureAnalyzerDecimal.decimal(StyleFeatureAnalyzerJsDistance.jsDistance(
                        target.distribution(), current.distribution(), alpha.doubleValue()
                )));
            }
            case ROBUST_L1_INPUT -> {
                primary = StyleFeatureAnalyzerDecimal.decimal(StyleFeatureAnalyzerL1.l1(target, current));
                diagnostics.put("requires_profile_calibration", true);
            }
            case COSINE_DISTANCE -> {
                primary = StyleFeatureAnalyzerDecimal.decimal(StyleFeatureAnalyzerCosineDistance.cosineDistance(target.embedding(), current.embedding()));
                diagnostics.put("content_reduced_only", true);
                diagnostics.put("secondary_evidence_only", true);
            }
            default -> throw new IllegalStateException("Unsupported style distance metric");
        }
        diagnostics.put("top_contributors", StyleFeatureAnalyzerTopContributors.topContributors(target, current, 10));
        return new StyleChannelDistance(
                channel,
                channel.featureVersion(),
                channel.primaryMetric(),
                primary,
                channel.required(),
                diagnostics
        );
    }
}
