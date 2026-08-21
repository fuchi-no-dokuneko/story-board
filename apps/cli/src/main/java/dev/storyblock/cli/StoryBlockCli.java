package dev.storyblock.cli;

import java.util.Arrays;

public final class StoryBlockCli {
    private StoryBlockCli() {
    }

    public static void main(String[] args) {
        if (args.length == 0 || Arrays.asList(args).contains("--help")) {
            System.out.println("StoryBlock CLI: replay verification, backup, and restore commands are added by their owning ADRs.");
            return;
        }
        throw new IllegalArgumentException("Unknown command: " + args[0]);
    }
}
