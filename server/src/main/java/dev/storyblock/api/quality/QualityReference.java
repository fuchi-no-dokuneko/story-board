package dev.storyblock.api.quality;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.quality.*;
import java.io.IOException;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class QualityReference {
    private final QualityInputs inputs;
    private QualityContract cachedContract;
    private QualityCalibration cached;
    public QualityReference(@Value("${storyblock.quality.corpus:server/config/content-config/quality.json}") String path) throws IOException {
        Path file = Path.of(System.getProperty("storyblock.root", ".")).resolve(path);
        inputs = Files.exists(file) ? CanonicalJson.parse(Files.readAllBytes(file), QualityInputs.class) : QualityInputs.empty();
    }
    public QualityContract contract() { return inputs.contract(); }
    public synchronized QualityCalibration calibration(QualityContract contract) {
        if (!contract.equals(cachedContract)) {
            cached = QualityCalibrator.fit(inputs.sources(),contract); cachedContract = contract;
        }
        return cached;
    }
}
