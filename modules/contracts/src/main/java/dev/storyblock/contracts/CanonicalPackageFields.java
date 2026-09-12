package dev.storyblock.contracts;

import java.util.Set;
import java.util.regex.Pattern;

final class CanonicalPackageFields {
    static final Set<String> ROOT_FIELDS = Set.of(
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
    static final Pattern MEDIA_TYPE = Pattern.compile(
            "[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+"
    );


}
