package dev.storyblock.worker.llm;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.rewrite.RewriteWorkerInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

final class StandardIoRewriteRunner {
    static final int MAX_INPUT_BYTES = 128 * 1024;

    private final RewriteProposalGenerator generator;
    private final Clock clock;

    StandardIoRewriteRunner(RewriteProposalGenerator generator, Clock clock) {
        this.generator = Objects.requireNonNull(generator, "generator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    void run(InputStream input, OutputStream output)
            throws IOException, InterruptedException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        RewriteWorkerInput job = parse(readBounded(input));
        Instant createdAt = Instant.now(clock);
        RewriteTextProposal proposal = generator.generate(job, createdAt);
        output.write(CanonicalJson.bytes(proposal.canonicalValue()));
        output.write('\n');
        output.flush();
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        byte[] value = input.readNBytes(MAX_INPUT_BYTES + 1);
        if (value.length > MAX_INPUT_BYTES) {
            throw new LlmWorkerProtocolException(
                    "Rewrite worker input exceeds the byte limit"
            );
        }
        int length = value.length;
        if (length > 0 && value[length - 1] == '\n') {
            length--;
            if (length > 0 && value[length - 1] == '\r') {
                length--;
            }
        }
        if (length == 0) {
            throw new LlmWorkerProtocolException("Rewrite worker input is empty");
        }
        return length == value.length ? value : Arrays.copyOf(value, length);
    }

    private static RewriteWorkerInput parse(byte[] value) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = CanonicalJson.mapper().readValue(
                    value, Map.class
            );
            if (!MessageDigest.isEqual(value, CanonicalJson.bytes(parsed))) {
                throw new IllegalArgumentException("Input is not canonical JSON");
            }
            return RewriteWorkerInput.fromCanonical(parsed);
        } catch (RuntimeException invalid) {
            throw new LlmWorkerProtocolException(
                    "Rewrite worker input does not match the canonical contract"
            );
        }
    }
}
