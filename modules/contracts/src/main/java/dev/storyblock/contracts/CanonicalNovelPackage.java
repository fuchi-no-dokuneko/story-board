package dev.storyblock.contracts;
import java.util.*;
public final class CanonicalNovelPackage implements CanonicalManifestTypes, CanonicalRevisionTypes, CanonicalArtifactTypes {
    public static final String PACKAGE_VERSION = "1.0.0";
    public static final int MAX_REVISIONS = 100_000, MAX_ARTIFACTS = 10_000;
    public static final int MAX_ARTIFACT_BYTES = 16 * 1024 * 1024;
    public static final int MAX_PACKAGE_BYTES = 64 * 1024 * 1024;
    private final Manifest manifest;
    private final List<RevisionEntry> revisions;
    private final List<OperationEntry> operations;
    private final List<ArtifactEntry> artifacts;
    private final byte[] canonicalBytes;
    CanonicalNovelPackage(
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
        CanonicalPackageValidation.validate(this);
        byte[] encoded = CanonicalJson.bytes(CanonicalPackageEncoding.encode(this));
        if (encoded.length > MAX_PACKAGE_BYTES) {
            throw new CanonicalPackageException("Canonical package exceeds the size limit");
        }
        this.canonicalBytes = encoded;
    }


    public static CanonicalNovelPackage assemble(List<RevisionEntry> revisions,
            List<OperationEntry> operations, List<ArtifactEntry> artifacts) {
        return CanonicalPackageAssembly.assemble(revisions, operations, artifacts);
    }
    public static CanonicalNovelPackage genesis(CanonicalRevision revision) {
        return CanonicalPackageAssembly.genesis(revision);
    }
    public static CanonicalNovelPackage parse(byte[] json) { return CanonicalPackageParser.parse(json); }
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

}
