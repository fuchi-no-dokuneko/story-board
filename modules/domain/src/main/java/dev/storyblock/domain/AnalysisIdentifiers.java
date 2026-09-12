package dev.storyblock.domain;



public interface AnalysisIdentifiers {
    public record JobId(String value) {
        public JobId {
            value = StableIds.require(value, "job");
        }

        public static JobId create() {
            return new JobId(StableIds.generate("job"));
        }
    }

    public record StyleAnalysisId(String value) {
        public StyleAnalysisId {
            value = StableIds.require(value, "ana");
        }

        public static StyleAnalysisId create() {
            return new StyleAnalysisId(StableIds.generate("ana"));
        }
    }

    public record ArtifactId(String value) {
        public ArtifactId {
            value = StableIds.require(value, "art");
        }

        public static ArtifactId create() {
            return new ArtifactId(StableIds.generate("art"));
        }

        public static ArtifactId derive(StyleAnalysisId analysisId, String discriminator) {
            return new ArtifactId(StableIds.derive(
                    "art", analysisId.value(), discriminator
            ));
        }
    }

    public record StyleProfileId(String value) {
        public StyleProfileId {
            value = StableIds.require(value, "spf");
        }

        public static StyleProfileId create() {
            return new StyleProfileId(StableIds.generate("spf"));
        }
    }

    public record StyleProfileVersionId(String value) {
        public StyleProfileVersionId {
            value = StableIds.require(value, "spv");
        }

        public static StyleProfileVersionId create() {
            return new StyleProfileVersionId(StableIds.generate("spv"));
        }
    }

    public record StyleLifecycleEventId(String value) {
        public StyleLifecycleEventId {
            value = StableIds.require(value, "sle");
        }

        public static StyleLifecycleEventId create() {
            return new StyleLifecycleEventId(StableIds.generate("sle"));
        }
    }
}
