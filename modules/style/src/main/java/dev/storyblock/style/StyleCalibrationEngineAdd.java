package dev.storyblock.style;

import java.util.ArrayList;
import java.util.Map;
import static dev.storyblock.style.StyleCalibrationEngine.CalibrationGroup;

final class StyleCalibrationEngineAdd {
    static void add(
            Map<String, CalibrationGroup> groups,
            StyleStratum stratum,
            StyleWindowFeatures window
    ) {
        groups.computeIfAbsent(
                stratum.canonicalKey(), ignored -> new CalibrationGroup(
                        stratum, new ArrayList<>()
                )
        ).windows().add(window);
    }
}
