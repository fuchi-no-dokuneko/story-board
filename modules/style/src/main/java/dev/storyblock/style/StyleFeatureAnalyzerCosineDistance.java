package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.List;

final class StyleFeatureAnalyzerCosineDistance {
    static double cosineDistance(List<BigDecimal> left, List<BigDecimal> right) {
        if (left.size() != right.size()) {
            throw new IllegalArgumentException("Style embedding dimensions do not match");
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index).doubleValue();
            double rightValue = right.get(index).doubleValue();
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            throw new IllegalArgumentException("Style embedding cannot be a zero vector");
        }
        double similarity = dot / (StrictMath.sqrt(leftNorm) * StrictMath.sqrt(rightNorm));
        return Math.max(0, Math.min(2, 1 - similarity));
    }
}
