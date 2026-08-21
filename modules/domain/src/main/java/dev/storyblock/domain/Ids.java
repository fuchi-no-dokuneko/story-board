package dev.storyblock.domain;

public final class Ids {
    private Ids() {
    }

    public record NovelId(String value) {
        public NovelId {
            value = StableIds.require(value, "nov");
        }

        public static NovelId create() {
            return new NovelId(StableIds.generate("nov"));
        }
    }

    public record ChapterId(String value) {
        public ChapterId {
            value = StableIds.require(value, "ch");
        }

        public static ChapterId create() {
            return new ChapterId(StableIds.generate("ch"));
        }
    }

    public record SceneId(String value) {
        public SceneId {
            value = StableIds.require(value, "scn");
        }

        public static SceneId create() {
            return new SceneId(StableIds.generate("scn"));
        }
    }

    public record BlockId(String value) {
        public BlockId {
            value = StableIds.require(value, "blk");
        }

        public static BlockId create() {
            return new BlockId(StableIds.generate("blk"));
        }
    }

    public record BlockVersionId(String value) {
        public BlockVersionId {
            value = StableIds.require(value, "blv");
        }

        public static BlockVersionId create() {
            return new BlockVersionId(StableIds.generate("blv"));
        }
    }

    public record RevisionId(String value) {
        public RevisionId {
            value = StableIds.require(value, "rev");
        }

        public static RevisionId create() {
            return new RevisionId(StableIds.generate("rev"));
        }
    }

    public record OperationId(String value) {
        public OperationId {
            value = StableIds.require(value, "op");
        }

        public static OperationId create() {
            return new OperationId(StableIds.generate("op"));
        }
    }

    public record JobId(String value) {
        public JobId {
            value = StableIds.require(value, "job");
        }

        public static JobId create() {
            return new JobId(StableIds.generate("job"));
        }
    }

    public record ProposalId(String value) {
        public ProposalId {
            value = StableIds.require(value, "prp");
        }

        public static ProposalId create() {
            return new ProposalId(StableIds.generate("prp"));
        }
    }
}
