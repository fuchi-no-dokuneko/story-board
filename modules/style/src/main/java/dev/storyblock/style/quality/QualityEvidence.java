package dev.storyblock.style.quality;

public record QualityEvidence(int start, int end, String text, String reason) {
    static QualityEvidence span(QualityWindow window, int start, int end, String reason) {
        return new QualityEvidence(start, end, window.text().substring(start, end), reason);
    }
}
