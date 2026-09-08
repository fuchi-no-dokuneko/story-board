package dev.storyblock.contracts;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class CanonicalNovelPackage {
    public static final String PACKAGE_VERSION = "1.0.0";
    public static final int MAX_REVISIONS = 100_000;
    public static final int MAX_ARTIFACTS = 10_000;
    public static final int MAX_ARTIFACT_BYTES = 16 * 1024 * 1024;
    public static final int MAX_PACKAGE_BYTES = 64 * 1024 * 1024;

    private static final Set<String> ROOT_FIELDS = Set.of(
            "package_version", "manifest", "revisions", "operations", "artifacts"
    );
    static final Set<String> MANIFEST_FIELDS = Set.of(
            "novel_id", "schema_version", "head_revision_id", "head_sequence",
            "head_hash", "revision_count", "operation_count", "artifact_count"
    );
    static final Set<String> REVISION_FIELDS = Set.of("sequence", "document");
    static final Set<String> OPERATION_FIELDS = Set.of(
            "sequence", "operation_hash", "operation", "result_revision_id",
            "result_hash", "committed_at"
    );
    static final Set<String> ARTIFACT_FIELDS = Set.of(
            "artifact_id", "revision_id", "kind", "media_type", "codec",
            "content_hash", "size_bytes", "created_at", "content_base64"
    );
    static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");
    static final Pattern TOKEN = Pattern.compile("[a-z][a-z0-9.-]{1,63}");
    private static final Pattern MEDIA_TYPE = Pattern.compile(
            "[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+"
    );

    final Manifest manifest;
    private final List<RevisionEntry> revisions;
    private final List<OperationEntry> operations;
    private final List<ArtifactEntry> artifacts;
    private final byte[] canonicalBytes;

    private CanonicalNovelPackage(
            String packageVersion,
            Manifest manifest,
            List<RevisionEntry> revisions,
            List<OperationEntry> operations,
            List<ArtifactEntry> artifacts
    ) {
        if (!PACKAGE_VERSION.equals(packageVersion)) {
            throw new CanonicalPackageException(
                    "Unsupported package_version " + packageVersion
            );
        }
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.revisions = List.copyOf(revisions);
        this.operations = List.copyOf(operations);
        this.artifacts = artifacts.stream()
                .sorted(Comparator.comparing(entry -> entry.artifactId().value()))
                .toList();
        validatePackage();
        byte[] encoded = CanonicalJson.bytes(toCanonicalMap());
        if (encoded.length > MAX_PACKAGE_BYTES) {
            throw new CanonicalPackageException("Canonical package exceeds the size limit");
        }
        this.canonicalBytes = encoded;
    }

    public static CanonicalNovelPackage assemble(
            List<RevisionEntry> revisions,
            List<OperationEntry> operations,
            List<ArtifactEntry> artifacts
    ) {
        Objects.requireNonNull(revisions, "revisions");
        if (revisions.isEmpty()) {
            throw new CanonicalPackageException("Canonical package has no revisions");
        }
        RevisionEntry head = revisions.getLast();
        RevisionManifest headManifest = NarrativeCanonicalMapper.fromCanonical(head.revision());
        Manifest manifest = new Manifest(
                headManifest.novel().id(),
                CanonicalRevision.SCHEMA_VERSION,
                headManifest.id(),
                head.sequence(),
                head.revision().contentHash(),
                revisions.size(),
                operations.size(),
                artifacts.size()
        );
        return new CanonicalNovelPackage(
                PACKAGE_VERSION, manifest, revisions, operations, artifacts
        );
    }

    public static CanonicalNovelPackage genesis(CanonicalRevision revision) {
        Objects.requireNonNull(revision, "revision");
        RevisionManifest manifest = NarrativeCanonicalMapper.fromCanonical(revision);
        if (manifest.parentId() != null) {
            throw new CanonicalPackageException(
                    "A standalone canonical revision must be a genesis revision"
            );
        }
        return assemble(List.of(new RevisionEntry(0, revision)), List.of(), List.of());
    }

    public static CanonicalNovelPackage parse(byte[] json) {
        Objects.requireNonNull(json, "json");
        if (json.length == 0 || json.length > MAX_PACKAGE_BYTES) {
            throw new CanonicalPackageException("Canonical package size is invalid");
        }
        final Map<String, Object> root;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = CanonicalJson.mapper().readValue(json, Map.class);
            if (parsed == null) {
                throw new CanonicalPackageException("Canonical package must be an object");
            }
            root = parsed;
        } catch (RuntimeException failure) {
            throw new CanonicalPackageException("Canonical package JSON is malformed", failure);
        }
        CanonicalNovelPackageRequireKeys.requireKeys(root, ROOT_FIELDS, "package");

        Manifest manifest = CanonicalNovelPackageParseManifest.parseManifest(object(root.get("manifest"), "manifest"));
        List<RevisionEntry> revisions = CanonicalNovelPackageArray.array(root.get("revisions"), "revisions").stream()
                .map(value -> CanonicalNovelPackageParseRevision.parseRevision(object(value, "revision")))
                .toList();
        List<OperationEntry> operations = CanonicalNovelPackageArray.array(root.get("operations"), "operations").stream()
                .map(value -> CanonicalNovelPackageParseOperation.parseOperation(object(value, "operation entry")))
                .toList();
        List<ArtifactEntry> artifacts = CanonicalNovelPackageArray.array(root.get("artifacts"), "artifacts").stream()
                .map(value -> CanonicalNovelPackageParseArtifact.parseArtifact(object(value, "artifact")))
                .toList();
        return new CanonicalNovelPackage(
                CanonicalNovelPackageString.string(root, "package_version", "package"),
                manifest,
                revisions,
                operations,
                artifacts
        );
    }

    public Manifest manifest() {
        return manifest;
    }

    public List<RevisionEntry> revisions() {
        return revisions;
    }

    public List<OperationEntry> operations() {
        return operations;
    }

    public List<ArtifactEntry> artifacts() {
        return artifacts;
    }

    public byte[] bytes() {
        return canonicalBytes.clone();
    }

    public String packageHash() {
        return CanonicalJson.hashBytes(canonicalBytes);
    }

    private void validatePackage() {
        if (revisions.isEmpty()) {
            throw new CanonicalPackageException("Canonical package has no revisions");
        }
        if (revisions.size() > MAX_REVISIONS) {
            throw new CanonicalPackageException("Canonical package has too many revisions");
        }
        if (operations.size() != revisions.size() - 1) {
            throw new CanonicalPackageException(
                    "Every revision after genesis must have exactly one operation"
            );
        }
        if (artifacts.size() > MAX_ARTIFACTS) {
            throw new CanonicalPackageException("Canonical package has too many artifacts");
        }

        Set<Ids.RevisionId> revisionIds = new HashSet<>();
        List<RevisionManifest> manifests = new ArrayList<>(revisions.size());
        for (int index = 0; index < revisions.size(); index++) {
            RevisionEntry entry = revisions.get(index);
            if (entry.sequence() != index) {
                throw new CanonicalPackageException("Revision sequences must be contiguous");
            }
            RevisionManifest current = NarrativeCanonicalMapper.fromCanonical(entry.revision());
            manifests.add(current);
            if (!current.novel().id().equals(manifest.novelId())) {
                throw new CanonicalPackageException("Revision belongs to another novel");
            }
            if (!revisionIds.add(current.id())) {
                throw new CanonicalPackageException("Duplicate revision ID " + current.id().value());
            }
            Ids.RevisionId expectedParent = index == 0 ? null : manifests.get(index - 1).id();
            if (!Objects.equals(expectedParent, current.parentId())) {
                throw new CanonicalPackageException("Revision lineage is not a single ordered chain");
            }
        }

        Set<Ids.OperationId> operationIds = new HashSet<>();
        Set<String> idempotencyKeys = new HashSet<>();
        for (int index = 0; index < operations.size(); index++) {
            OperationEntry entry = operations.get(index);
            long expectedSequence = index + 1L;
            if (entry.sequence() != expectedSequence) {
                throw new CanonicalPackageException("Operation sequences must be contiguous");
            }
            EditOperation operation = entry.operation();
            RevisionEntry base = revisions.get(index);
            RevisionEntry result = revisions.get(index + 1);
            RevisionManifest resultManifest = manifests.get(index + 1);
            if (!operation.context().novelId().equals(manifest.novelId())
                    || !operation.context().baseRevisionId().equals(manifests.get(index).id())
                    || !operation.context().expectedHeadHash().equals(base.revision().contentHash())
                    || !entry.resultRevisionId().equals(resultManifest.id())
                    || !entry.resultHash().equals(result.revision().contentHash())
                    || !entry.committedAt().equals(resultManifest.createdAt())) {
                throw new CanonicalPackageException(
                        "Operation does not match its base and result revisions"
                );
            }
            if (!EditOperationCanonicalMapper.hash(operation).equals(entry.operationHash())) {
                throw new CanonicalPackageException("Operation hash does not match its payload");
            }
            if (!operationIds.add(operation.context().operationId())) {
                throw new CanonicalPackageException("Duplicate operation ID");
            }
            if (!idempotencyKeys.add(operation.context().idempotencyKey())) {
                throw new CanonicalPackageException("Duplicate operation idempotency key");
            }
        }

        RevisionEntry head = revisions.getLast();
        RevisionManifest headManifest = manifests.getLast();
        if (!CanonicalRevision.SCHEMA_VERSION.equals(manifest.schemaVersion())
                || !manifest.headRevisionId().equals(headManifest.id())
                || manifest.headSequence() != head.sequence()
                || !manifest.headHash().equals(head.revision().contentHash())
                || manifest.revisionCount() != revisions.size()
                || manifest.operationCount() != operations.size()
                || manifest.artifactCount() != artifacts.size()) {
            throw new CanonicalPackageException("Package manifest does not match its contents");
        }

        Set<Ids.ArtifactId> artifactIds = new HashSet<>();
        for (ArtifactEntry artifact : artifacts) {
            if (!artifactIds.add(artifact.artifactId())) {
                throw new CanonicalPackageException("Duplicate artifact ID");
            }
            if (!revisionIds.contains(artifact.revisionId())) {
                throw new CanonicalPackageException("Artifact references an unknown revision");
            }
        }
    }

    private Map<String, Object> toCanonicalMap() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("package_version", PACKAGE_VERSION);
        root.put("manifest", CanonicalNovelPackageManifestMap.manifestMap(manifest));
        root.put("revisions", revisions.stream().map(CanonicalNovelPackageRevisionMap::revisionMap).toList());
        root.put("operations", operations.stream().map(CanonicalNovelPackageOperationMap::operationMap).toList());
        root.put("artifacts", artifacts.stream().map(CanonicalNovelPackageArtifactMap::artifactMap).toList());
        return Map.copyOf(root);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new CanonicalPackageException(path + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new CanonicalPackageException(path + " contains a non-string key");
            }
        }
        return (Map<String, Object>) map;
    }

    public record Manifest(
            Ids.NovelId novelId,
            String schemaVersion,
            Ids.RevisionId headRevisionId,
            long headSequence,
            String headHash,
            int revisionCount,
            int operationCount,
            int artifactCount
    ) {
        public Manifest {
            Objects.requireNonNull(novelId, "novelId");
            Objects.requireNonNull(schemaVersion, "schemaVersion");
            Objects.requireNonNull(headRevisionId, "headRevisionId");
            if (headSequence < 0 || revisionCount < 1
                    || operationCount < 0 || artifactCount < 0) {
                throw new CanonicalPackageException("Package manifest counts are invalid");
            }
            CanonicalNovelPackageRequireHash.requireHash(headHash, "Manifest head hash");
        }
    }

    public record RevisionEntry(long sequence, CanonicalRevision revision) {
        public RevisionEntry {
            if (sequence < 0) {
                throw new CanonicalPackageException("Revision sequence cannot be negative");
            }
            Objects.requireNonNull(revision, "revision");
        }
    }

    public record OperationEntry(
            long sequence,
            String operationHash,
            EditOperation operation,
            Ids.RevisionId resultRevisionId,
            String resultHash,
            Instant committedAt
    ) {
        public OperationEntry {
            if (sequence < 1) {
                throw new CanonicalPackageException("Operation sequence must be positive");
            }
            CanonicalNovelPackageRequireHash.requireHash(operationHash, "Operation hash");
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(resultRevisionId, "resultRevisionId");
            CanonicalNovelPackageRequireHash.requireHash(resultHash, "Operation result hash");
            Objects.requireNonNull(committedAt, "committedAt");
        }
    }

    public record ArtifactEntry(
            Ids.ArtifactId artifactId,
            Ids.RevisionId revisionId,
            String kind,
            String mediaType,
            String codec,
            String contentHash,
            byte[] content,
            Instant createdAt
    ) {
        public ArtifactEntry {
            Objects.requireNonNull(artifactId, "artifactId");
            Objects.requireNonNull(revisionId, "revisionId");
            kind = CanonicalNovelPackageRequireToken.requireToken(kind, "Artifact kind");
            codec = CanonicalNovelPackageRequireToken.requireToken(codec, "Artifact codec");
            if (mediaType == null || !MEDIA_TYPE.matcher(mediaType).matches()) {
                throw new CanonicalPackageException("Artifact media type is invalid");
            }
            CanonicalNovelPackageRequireHash.requireHash(contentHash, "Artifact content hash");
            content = Objects.requireNonNull(content, "content").clone();
            if (content.length > MAX_ARTIFACT_BYTES) {
                throw new CanonicalPackageException("Artifact exceeds the package size limit");
            }
            if (!CanonicalJson.hashBytes(content).equals(contentHash)) {
                throw new CanonicalPackageException("Artifact hash does not match content");
            }
            Objects.requireNonNull(createdAt, "createdAt");
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
