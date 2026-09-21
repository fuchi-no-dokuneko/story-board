package dev.storyblock.worker.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.quality.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

final class QualityCommand {
    static void run(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4)
            throw new IllegalArgumentException("Usage / 用法: --quality input.txt output.json [corpus.json]");
        var inputs = args.length == 4 ? CanonicalJson.parse(Files.readAllBytes(Path.of(args[3])),QualityInputs.class) : QualityInputs.empty();
        var calibration = QualityCalibrator.fit(inputs.sources(),inputs.contract());
        var report = new QualityAnalyzer().analyze(Files.readString(Path.of(args[1]),StandardCharsets.UTF_8),inputs.contract(),calibration);
        var output = Path.of(args[2]).toAbsolutePath(); Files.createDirectories(output.getParent());
        var pending = Files.createTempFile(output.getParent(),".quality-",".json");
        try {
            Files.write(pending,CanonicalJson.bytes(report));
            Files.move(pending,output,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(pending); }
        System.out.println("Quality report saved / 品質報告已儲存: " + output);
    }
}
