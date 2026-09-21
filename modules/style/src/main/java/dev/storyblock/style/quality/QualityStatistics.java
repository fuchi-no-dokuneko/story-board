package dev.storyblock.style.quality;

import java.util.*;

public record QualityStatistics(Double wordFrequencyJsd, Double mattr, int mattrWindows) {
    static QualityStatistics calculate(QualityWindow window, QualityContract contract, QualityCalibration calibration) {
        var words = window.words();
        var counts = new HashMap<String,Integer>();
        for (var word : words) counts.merge(word.normalized(),1,Integer::sum);
        Double jsd = null;
        if (!words.isEmpty() && !calibration.humanWords().isEmpty()) {
            var keys = new HashSet<>(counts.keySet()); keys.addAll(calibration.humanWords().keySet());
            double divergence = 0;
            for (String key : keys) {
                double p = counts.getOrDefault(key,0)/(double)words.size(), q = calibration.humanWords().getOrDefault(key,0.0);
                double m = (p + q)/2;
                if (p > 0) divergence += p * Math.log(p/m)/Math.log(2)/2;
                if (q > 0) divergence += q * Math.log(q/m)/Math.log(2)/2;
            }
            jsd = divergence;
        }
        int width = contract.mattrWords(), windows = Math.max(0, words.size()-width+1);
        double total = 0; counts.clear();
        for (int i = 0; i < words.size(); i++) {
            counts.merge(words.get(i).normalized(),1,Integer::sum);
            if (i >= width) { String old = words.get(i-width).normalized(); if (counts.merge(old,-1,Integer::sum) == 0) counts.remove(old); }
            if (i+1 >= width) total += counts.size()/(double)width;
        }
        return new QualityStatistics(jsd, windows == 0 ? null : total/windows, windows);
    }
}
