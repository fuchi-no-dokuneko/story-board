package dev.storyblock.contracts;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
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
    private static final Set<String> MANIFEST_FIELDS = Set.of(
            "novel_id", "schema_version", "head_revision_id", "head_sequence",
            "head_hash", "revision_count", "operation_count", "artifact_count"
    );
    private static final Set<String> REVISION_FIELDS = Set.of("sequence", "document");
    private static final Set<String> OPERATION_FIELDS = Set.of(
            "sequence", "operation_hash", "operation", "result_revision_id",
            "result_hash", "committed_at"
    );
    private static final Set<String> ARTIFACT_FIELDS = Set.of(
            "artifact_id", "revision_id", "kind", "media_type", "codec",
            "content_hash", "size_bytes", "created_at", "content_base64"
    );
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Pattern TOKEN = Pattern.compile("[a-z][a-z0-9.-]{1,63}");
    private static final Pattern MEDIA_TYPE = Pattern.compile(
            "[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+"
    );

    private final Manifest manifest;
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
        requireKeys(root, ROOT_FIELDS, "package");

        Manifest manifest = parseManifest(object(root.get("manifest"), "manifest"));
        List<RevisionEntry> revisions = array(root.get("revisions"), "revisions").stream()
                .map(value -> parseRevision(object(value, "revision")))
                .toList();
        List<OperationEntry> operations = array(root.get("operations"), "operations").stream()
                .map(value -> parseOperation(object(value, "operation entry")))
                .toList();
        List<ArtifactEntry> artifacts = array(root.get("artifacts"), "artifacts").stream()
                .map(value -> parseArtifact(object(value, "artifact")))
                .toList();
        return new CanonicalNovelPackage(
                string(root, "package_version", "package"),
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
        root.put("manifest", manifestMap(manifest));
        root.put("revisions", revisions.stream().map(CanonicalNovelPackage::revisionMap).toList());
        root.put("operations", operations.stream().map(CanonicalNovelPackage::operationMap).toList());
        root.put("artifacts", artifacts.stream().map(CanonicalNovelPackage::artifactMap).toList());
        return Map.copyOf(root);
    }

    private static Map<String, Object> manifestMap(Manifest value) {
        return Map.of(
                "novel_id", value.novelId().value(),
                "schema_version", value.schemaVersion(),
                "head_revision_id", value.headRevisionId().value(),
                "head_sequence", value.headSequence(),
                "head_hash", value.headHash(),
                "revision_count", value.revisionCount(),
                "operation_count", value.operationCount(),
                "artifact_count", value.artifactCount()
        );
    }

    private static Map<String, Object> revisionMap(RevisionEntry value) {
        return Map.of(
                "sequence", value.sequence(),
                "document", value.revision().envelope()
        );
    }

    private static Map<String, Object> operationMap(OperationEntry value) {
        return Map.of(
                "sequence", value.sequence(),
                "operation_hash", value.operationHash(),
                "operation", EditOperationCanonicalMapper.toCanonical(value.operation()),
                "result_revision_id", value.resultRevisionId().value(),
                "result_hash", value.resultHash(),
                "committed_at", value.committedAt().toString()
        );
    }

    private static Map<String, Object> artifactMap(ArtifactEntry value) {
        return Map.of(
                "artifact_id", value.artifactId().value(),
                "revision_id", value.revisionId().value(),
                "kind", value.kind(),
                "media_type", value.mediaType(),
                "codec", value.codec(),
                "content_hash", value.contentHash(),
                "size_bytes", value.content().length,
                "created_at", value.createdAt().toString(),
                "content_base64", Base64.getEncoder().encodeToString(value.content())
        );
    }

    private static Manifest parseManifest(Map<String, Object> value) {
        requireKeys(value, MANIFEST_FIELDS, "manifest");
        return new Manifest(
                new Ids.NovelId(string(value, "novel_id", "manifest")),
                string(value, "schema_version", "manifest"),
                new Ids.RevisionId(string(value, "head_revision_id", "manifest")),
                exactLong(value.get("head_sequence"), "manifest.head_sequence"),
                string(value, "head_hash", "manifest"),
                exactInt(value.get("revision_count"), "manifest.revision_count"),
                exactInt(value.get("operation_count"), "manifest.operation_count"),
                exactInt(value.get("artifact_count"), "manifest.artifact_count")
        );
    }

    private static RevisionEntry parseRevision(Map<String, Object> value) {
        requireKeys(value, REVISION_FIELDS, "revision");
        Map<String, Object> document = object(value.get("document"), "revision.document");
        try {
            return new RevisionEntry(
                    exactLong(value.get("sequence"), "revision.sequence"),
                    CanonicalRevision.parseEnvelope(CanonicalJson.bytes(document))
            );
        } catch (IllegalArgumentException failure) {
            throw new CanonicalPackageException("Canonical package revision is invalid", failure);
        }
    }

    private static OperationEntry parseOperation(Map<String, Object> value) {
        requireKeys(value, OPERATION_FIELDS, "operation entry");
        try {
            return new OperationEntry(
                    exactLong(value.get("sequence"), "operation.sequence"),
                    string(value, "operation_hash", "operation entry"),
                    EditOperationCanonicalMapper.fromCanonical(
                            object(value.get("operation"), "operation")
                    ),
                    new Ids.RevisionId(string(
                            value, "result_revision_id", "operation entry"
                    )),
                    string(value, "result_hash", "operation entry"),
                    instant(value, "committed_at", "operation entry")
            );
        } catch (IllegalArgumentException failure) {
            throw new CanonicalPackageException("Canonical package operation is invalid", failure);
        }
    }

    private static ArtifactEntry parseArtifact(Map<String, Object> value) {
        requireKeys(value, ARTIFACT_FIELDS, "artifact");
        String encoded = string(value, "content_base64", "artifact");
        final byte[] content;
        try {
            content = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException failure) {
            throw new CanonicalPackageException("Artifact content_base64 is invalid", failure);
        }
        if (!Base64.getEncoder().encodeToString(content).equals(encoded)) {
            throw new CanonicalPackageException("Artifact base64 must use canonical encoding");
        }
        if (exactInt(value.get("size_bytes"), "artifact.size_bytes") != content.length) {
            throw new CanonicalPackageException("Artifact size_bytes does not match content");
        }
        return new ArtifactEntry(
                new Ids.ArtifactId(string(value, "artifact_id", "artifact")),
                new Ids.RevisionId(string(value, "revision_id", "artifact")),
                string(value, "kind", "artifact"),
                string(value, "media_type", "artifact"),
                string(value, "codec", "artifact"),
                string(value, "content_hash", "artifact"),
                content,
                instant(value, "created_at", "artifact")
        );
    }

    private static Instant instant(Map<String, Object> value, String field, String path) {
        String text = string(value, field, path);
        try {
            Instant instant = Instant.parse(text);
            if (!instant.toString().equals(text)) {
                throw new CanonicalPackageException(path + "." + field + " is not canonical UTC");
            }
            return instant;
        } catch (DateTimeParseException failure) {
            throw new CanonicalPackageException(path + "." + field + " is invalid", failure);
        }
    }

    private static long exactLong(Object value, String path) {
        if (!(value instanceof Number number)) {
            throw new CanonicalPackageException(path + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new CanonicalPackageException(path + " must be an exact integer", failure);
        }
    }

    private static int exactInt(Object value, String path) {
        try {
            return Math.toIntExact(exactLong(value, path));
        } catch (ArithmeticException failure) {
            throw new CanonicalPackageException(path + " is outside the integer range", failure);
        }
    }

    private static String string(Map<String, Object> value, String field, String path) {
        Object entry = value.get(field);
        if (!(entry instanceof String text)) {
            throw new CanonicalPackageException(path + "." + field + " must be a string");
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String path) {
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

    private static List<Object> array(Object value, String path) {
        if (!(value instanceof List<?> list)) {
            throw new CanonicalPackageException(path + " must be an array");
        }
        return new ArrayList<>(list);
    }

    private static void requireKeys(
            Map<String, Object> value,
            Set<String> required,
            String path
    ) {
        for (String field : required) {
            if (!value.containsKey(field)) {
                throw new CanonicalPackageException(path + " is missing " + field);
            }
        }
        for (String field : value.keySet()) {
            if (!required.contains(field)) {
                throw new CanonicalPackageException(path + " contains unknown field " + field);
            }
        }
    }

    private static void requireHash(String value, String field) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new CanonicalPackageException(field + " must be lowercase SHA-256");
        }
    }

    private static String requireToken(String value, String field) {
        if (value == null || !TOKEN.matcher(value).matches()) {
            throw new CanonicalPackageException(field + " is not a canonical token");
        }
        return value;
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
            requireHash(headHash, "Manifest head hash");
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
            requireHash(operationHash, "Operation hash");
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(resultRevisionId, "resultRevisionId");
            requireHash(resultHash, "Operation result hash");
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
            kind = requireToken(kind, "Artifact kind");
            codec = requireToken(codec, "Artifact codec");
            if (mediaType == null || !MEDIA_TYPE.matcher(mediaType).matches()) {
                throw new CanonicalPackageException("Artifact media type is invalid");
            }
            requireHash(contentHash, "Artifact content hash");
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
