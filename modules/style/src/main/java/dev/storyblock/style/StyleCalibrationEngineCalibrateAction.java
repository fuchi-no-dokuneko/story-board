package dev.storyblock.style;

import java.util.List;
import static dev.storyblock.style.StyleCalibrationEngine.CalibrationGroup;

final class StyleCalibrationEngineCalibrateAction {
    static StyleCalibrationProfile calibrate(StyleCalibrationEngine self, String targetCorpusHash, StyleWindowConfiguration configuration, List<StyleWindowFeatures> windows)  {
        return StyleCalibrationEngineCalibrateActionCalibrateFactory.calibrate(self, targetCorpusHash, configuration, windows);
    }

    static StyleStratumCalibration calibrate(StyleCalibrationEngine self, CalibrationGroup group)  {
        return StyleCalibrationEngineCalibrateActionCalibrateFactory.calibrate(self, group);
    }
}
