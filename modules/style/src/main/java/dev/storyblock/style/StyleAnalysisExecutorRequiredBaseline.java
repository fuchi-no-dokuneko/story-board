package dev.storyblock.style;



final class StyleAnalysisExecutorRequiredBaseline {
    static StyleFeatureSet requiredBaseline(StyleFeatureSet source) {
        return new StyleFeatureSet(
                source.sourceHash(),
                source.contract(),
                source.channels().stream()
                        .filter(vector -> vector.channel().required())
                        .toList()
        );
    }
}
