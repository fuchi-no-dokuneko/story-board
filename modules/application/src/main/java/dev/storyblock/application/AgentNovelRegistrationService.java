package dev.storyblock.application;

import dev.storyblock.storage.CanonicalImportResult;
import java.util.Objects;
import java.util.regex.Pattern;

public final class AgentNovelRegistrationService {
    static final Pattern SENTENCE = Pattern.compile(
            "\\G\\s*(.*?(?:[。！？!?]+|…{1,2})[」』”’\\\"'）)】》〉〕］}]*?)",
            Pattern.DOTALL
    );

    final CanonicalTransferService transfers;
    final NovelCatalogService catalog;

    public AgentNovelRegistrationService(
            CanonicalTransferService transfers,
            NovelCatalogService catalog
    ) {
        this.transfers = Objects.requireNonNull(transfers, "transfers");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public Result register(
            AgentNovelRegistration request,
            String idempotencyKey
    ) {
        return AgentNovelRegistrationServiceRegisterAction.register(this, request, idempotencyKey);
    }

    public record Result(
            CanonicalImportResult importResult,
            NovelCatalogEntry novel
    ) {
    }
}
