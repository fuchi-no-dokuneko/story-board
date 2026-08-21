package dev.storyblock.storage.sqlite;

@FunctionalInterface
interface CommitFaultInjector {
    CommitFaultInjector NONE = stage -> {
    };

    void after(CommitStage stage);
}
