package dev.storyblock.storage.sqlite;

@FunctionalInterface
interface ImportFaultInjector {
    ImportFaultInjector NONE = ignored -> { };

    void after(ImportStage stage);
}
