package dev.storyblock.style.quality;

import dev.storyblock.nlp.zh.ChineseToken;
import java.util.*;

public final class SkeletonReuseMetric implements QualityMetric {
    public String id() { return "skeleton_reuse"; }
    public QualityMetricResult measure(QualityWindow window, QualityContract contract, QualityCalibration calibration) {
        var seen = new HashSet<List<String>>();
        var evidence = new ArrayList<QualityEvidence>();
        int total = 0, repeated = 0;
        for (var sentence : window.sentences()) {
            var skeleton = new ArrayList<String>(); int markers = 0;
            for (var token : sentence.tokens()) {
                if (token.source().kind() == ChineseToken.Kind.SPACE) continue;
                if (token.functionWord() || punctuation(token.source().text())) {
                    skeleton.add("marker:" + token.normalized()); markers++;
                } else if (skeleton.isEmpty() || !skeleton.getLast().equals("X")) skeleton.add("X");
            }
            if (markers < 2) continue;
            total++;
            if (!seen.add(List.copyOf(skeleton))) {
                repeated++;
                if (evidence.size() < contract.evidenceLimit()) evidence.add(QualityEvidence.span(window,
                        sentence.start(), sentence.end(), String.join(" ", skeleton)));
            }
        }
        if (total < contract.minSkeletonSentences()) return QualityMetricResult.unavailable(id(), QualityMetricResult.Status.INSUFFICIENT_DATA, total);
        return QualityMetricResult.result(id(), repeated / (double)total, total, evidence);
    }
    private static boolean punctuation(String text) {
        int type = Character.getType(text.codePointAt(0));
        return type == Character.CONNECTOR_PUNCTUATION || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION || type == Character.END_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION;
    }
}
