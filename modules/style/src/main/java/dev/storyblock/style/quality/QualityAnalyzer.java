package dev.storyblock.style.quality;

import dev.storyblock.nlp.zh.*;

public final class QualityAnalyzer {
    private final ChineseTokenizer tokenizer;
    public QualityAnalyzer() { this(new JiebaTokenizer()); }
    public QualityAnalyzer(ChineseTokenizer tokenizer) { this.tokenizer = java.util.Objects.requireNonNull(tokenizer); }
    public QualityIndexReport analyze(String text, QualityContract contract, QualityCalibration calibration) {
        var analysis = QualityAnalysis.analyze(text,contract,tokenizer);
        var windows = QualityAnalysis.windows(analysis,contract).stream().map(w -> new QualityIndexReport.WindowReport(
                w.start(),w.end(),w.words().size(),w.sentences().size(),QualityMetrics.measure(w,contract,calibration),
                QualityStatistics.calculate(w,contract,calibration))).toList();
        return new QualityIndexReport(contract,analysis.words().size(),analysis.sentences().size(),
                new QualityIndexReport.CalibrationSummary(calibration.humanSources(),calibration.aiSources(),
                        calibration.shortMaximum(),calibration.mediumMaximum()),
                QualityMetrics.measure(analysis,contract,calibration),QualityStatistics.calculate(analysis,contract,calibration),windows);
    }
}
