package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.RevisionManifest;
import java.sql.*;
import java.util.Map;

final class SqliteRevisionIdentities {
    static void claim(Connection connection, RevisionManifest revision, String hash) throws SQLException {
        String novel = revision.novel().id().value();
        var identities = SqliteIdentitySnapshot.read(connection, novel);
        identities.claim(novel, novel, null);
        identities.claim(revision.id().value(), novel, hash);
        for (var chapter : revision.novel().chapters()) {
            identities.claim(chapter.id().value(), novel, null);
            for (var scene : chapter.scenes()) {
                identities.claim(scene.id().value(), novel, null);
                for (var block : scene.blocks()) {
                    identities.claim(block.id().value(), novel, null);
                    String content = CanonicalJson.hashBytes(CanonicalJson.bytes(Map.of(
                            "text", block.text(), "meta", block.metadata().fields(), "extensions", block.extensions())));
                    identities.claim(block.versionId().value(), block.id().value(), content);
                }
            }
        }
    }
}
