package dev.storyblock.domain;



public interface MonitorIdentifiers {
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
