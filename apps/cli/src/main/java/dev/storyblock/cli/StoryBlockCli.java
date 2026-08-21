package dev.storyblock.cli;

import dev.storyblock.application.ReplayVerificationReport;
import dev.storyblock.application.ReplayService;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StoryBlockCli {
    private StoryBlockCli() {
    }

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) {
            System.exit(status);
        }
    }

    static int run(String[] args, PrintStream output, PrintStream error) {
        if (args.length == 0 || (args.length == 1 && "--help".equals(args[0]))) {
            output.println("Usage: storyblock replay-verify <sqlite-database>");
            return 0;
        }
        if (args.length != 2 || !"replay-verify".equals(args[0])) {
            error.println("Unknown command. Use --help for usage.");
            return 2;
        }

        Path databasePath = Path.of(args[1]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(databasePath)) {
            error.println("Replay verification failed: database file does not exist");
            return 1;
        }
        try (SqliteRevisionStore store = SqliteRevisionStore.open(databasePath)) {
            ReplayVerificationReport report = new ReplayService(store).verifyAllHeads();
            output.println(CanonicalJson.string(report.contractFields()));
            return report.valid() ? 0 : 1;
        } catch (RuntimeException | java.io.IOException exception) {
            error.println("Replay verification failed: " + exception.getMessage());
            return 1;
        }
    }
}
