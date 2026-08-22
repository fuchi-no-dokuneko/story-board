package dev.storyblock.rewrite;

public final class RewriteModule {
    public static final String INPUT_SCHEMA_VERSION = "rewrite-input-1.0.0";
    public static final String MODEL_PROTOCOL_VERSION = "rewrite-model-1.0.0";
    public static final String PROPOSAL_SCHEMA_VERSION = "rewrite-proposal-1.0.0";
    public static final int MAX_EDITABLE_BLOCKS = 64;
    public static final int MAX_CONTEXT_BLOCKS_PER_SIDE = 2;
    public static final int MAX_SOURCE_BLOCKS = MAX_EDITABLE_BLOCKS
            + (2 * MAX_CONTEXT_BLOCKS_PER_SIDE);
    public static final int MAX_FINDINGS = 64;
    public static final int MAX_STYLE_DIRECTIVES = 16;

    private RewriteModule() {
    }
}
