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

    public record ArtifactId(String value) {
        public ArtifactId {
            value = StableIds.require(value, "art");
        }

        public static ArtifactId create() {
            return new ArtifactId(StableIds.generate("art"));
        }
    }

    public record AccessKeyId(String value) {
        public AccessKeyId {
            value = StableIds.require(value, "key");
        }

        public static AccessKeyId create() {
            return new AccessKeyId(StableIds.generate("key"));
        }
    }

    public record AuditEventId(String value) {
        public AuditEventId {
            value = StableIds.require(value, "aud");
        }

        public static AuditEventId create() {
            return new AuditEventId(StableIds.generate("aud"));
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

    public record FindingId(String value) {
        public FindingId {
            value = StableIds.require(value, "fnd");
        }

        public static FindingId create() {
            return new FindingId(StableIds.generate("fnd"));
        }
    }

    public record MonitorRunId(String value) {
        public MonitorRunId {
            value = StableIds.require(value, "mrun");
        }

        public static MonitorRunId create() {
            return new MonitorRunId(StableIds.generate("mrun"));
        }
    }

    public sealed interface MonitorOutputId permits MonitorIssueId, MonitorProposalId {
        String value();

        static MonitorOutputId parse(String value) {
            if (value != null && value.startsWith("mis_")) {
                return new MonitorIssueId(value);
            }
            if (value != null && value.startsWith("mpr_")) {
                return new MonitorProposalId(value);
            }
            throw new IllegalArgumentException("Unsupported monitor output identifier");
        }
    }

    public record MonitorIssueId(String value) implements MonitorOutputId {
        public MonitorIssueId {
            value = StableIds.require(value, "mis");
        }

        public static MonitorIssueId create() {
            return new MonitorIssueId(StableIds.generate("mis"));
        }
    }

    public record MonitorProposalId(String value) implements MonitorOutputId {
        public MonitorProposalId {
            value = StableIds.require(value, "mpr");
        }

        public static MonitorProposalId create() {
            return new MonitorProposalId(StableIds.generate("mpr"));
        }
    }
}
