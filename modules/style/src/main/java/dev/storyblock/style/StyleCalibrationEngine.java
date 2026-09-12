package dev.storyblock.style;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class StyleCalibrationEngine {
    static final int SCALE = 12;
    static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    final StyleFeatureAnalyzer analyzer;

    public StyleCalibrationEngine() {
        this(new StyleFeatureAnalyzer());
    }

    StyleCalibrationEngine(StyleFeatureAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
    }

    public StyleCalibrationProfile calibrate(
            String targetCorpusHash,
            StyleWindowConfiguration configuration,
            List<StyleWindowFeatures> windows
    ) {
        return StyleCalibrationEngineCalibrateAction.calibrate(this, targetCorpusHash, configuration, windows);
    }

    StyleStratumCalibration calibrate(CalibrationGroup group) {
        return StyleCalibrationEngineCalibrateAction.calibrate(this, group);
    }

    record CalibrationGroup(
            StyleStratum stratum,
            List<StyleWindowFeatures> windows
    ) {
    }
}
