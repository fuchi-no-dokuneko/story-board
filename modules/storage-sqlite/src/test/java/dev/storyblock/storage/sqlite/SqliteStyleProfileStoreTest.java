package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.application.StyleProfileService;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.style.StyleCorpusSource;
import dev.storyblock.style.StyleCorpusSourceKind;
import dev.storyblock.style.StyleCalibrationProfile;
import dev.storyblock.style.StyleChannelCalibration;
import dev.storyblock.style.StyleFeatureAnalyzer;
import dev.storyblock.style.StyleFeatureContract;
import dev.storyblock.style.StyleFeatureSet;
import dev.storyblock.style.StyleLifecycleConflictException;
import dev.storyblock.style.StyleMaskingLexicon;
import dev.storyblock.style.StyleProfileScope;
import dev.storyblock.style.StyleProfileState;
import dev.storyblock.style.StyleProfileVersionContent;
import dev.storyblock.style.StyleScopeKind;
import dev.storyblock.style.StyleStratum;
import dev.storyblock.style.StyleStratumCalibration;
import dev.storyblock.style.StyleStatusPreconditionException;
import dev.storyblock.style.StyleWindowConfiguration;
import java.nio.file.Path;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteStyleProfileStoreTest {
    private static final Instant NOW = Instant.parse("2026-08-21T14:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void immutableLifecycleRequiresHumanPromotionAndKeepsOneReadyVersion()
            throws Exception {
        Path path = temporaryDirectory.resolve("style.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        Ids.NovelId novelId = genesis.novel().id();
        StyleProfileScope scope = new StyleProfileScope(
                novelId, StyleScopeKind.NOVEL, null
        );

        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            StyleProfileService service = new StyleProfileService(store);
            var created = service.createProfile(
                    "Author baseline",
                    scope,
                    "Owner-provided corpus",
                    "create-profile",
                    audit(0, "create-profile")
            );
            var replay = service.createProfile(
                    "Author baseline",
                    scope,
                    "Owner-provided corpus",
                    "create-profile",
                    audit(1, "replay-profile")
            );
            assertFalse(created.idempotentReplay());
            assertTrue(replay.idempotentReplay());
            assertEquals(created.profile(), replay.profile());
            assertThrows(IdempotencyConflictException.class, () ->
                    service.createProfile(
                            "Different baseline",
                            scope,
                            "Owner-provided corpus",
                            "create-profile",
                            audit(2, "profile-conflict")
                    )
            );

            var generated = service.createVersion(
                    created.profile().profileId(),
                    content(genesis, scope, StyleCorpusSourceKind.GENERATED),
                    created.profile().resourceHash(),
                    "create-generated-version",
                    audit(3, "create-generated-version")
            );
            assertEquals(StyleProfileState.DRAFT, generated.view().state());
            assertFalse(generated.view().canGateRewrites());
            assertThrows(StyleLifecycleConflictException.class, () ->
                    service.requireRewriteGate(
                            created.profile().profileId(),
                            generated.view().profileVersion().versionId()
                    )
            );

            var calibrating = service.transition(
                    created.profile().profileId(),
                    generated.view().profileVersion().versionId(),
                    StyleProfileState.CALIBRATING,
                    "Calibration corpus reviewed",
                    false,
                    generated.view().statusHash(),
                    "calibrate-generated-version",
                    audit(4, "calibrate-generated-version")
            );
            assertThrows(StyleLifecycleConflictException.class, () ->
                    service.transition(
                            created.profile().profileId(),
                            generated.view().profileVersion().versionId(),
                            StyleProfileState.READY,
                            "Promote generated baseline",
                            false,
                            calibrating.view().statusHash(),
                            "promote-generated-version",
                            audit(5, "promote-generated-without-confirmation")
                    )
            );
            var readyGenerated = service.transition(
                    created.profile().profileId(),
                    generated.view().profileVersion().versionId(),
                    StyleProfileState.READY,
                    "Explicitly approved generated baseline",
                    true,
                    calibrating.view().statusHash(),
                    "promote-generated-version",
                    audit(6, "promote-generated-version")
            );
            assertTrue(readyGenerated.view().canGateRewrites());
            assertEquals("style-owner", readyGenerated.view().approvedBy());
            assertEquals(
                    readyGenerated.view(),
                    service.requireRewriteGate(
                            created.profile().profileId(),
                            generated.view().profileVersion().versionId()
                    )
            );

            var ownerVersion = service.createVersion(
                    created.profile().profileId(),
                    content(genesis, scope, StyleCorpusSourceKind.OWNER),
                    created.profile().resourceHash(),
                    "create-owner-version",
                    audit(7, "create-owner-version")
            );
            var ownerCalibrating = service.transition(
                    created.profile().profileId(),
                    ownerVersion.view().profileVersion().versionId(),
                    StyleProfileState.CALIBRATING,
                    "Calibration complete",
                    false,
                    ownerVersion.view().statusHash(),
                    "calibrate-owner-version",
                    audit(8, "calibrate-owner-version")
            );
            var ownerReady = service.transition(
                    created.profile().profileId(),
                    ownerVersion.view().profileVersion().versionId(),
                    StyleProfileState.READY,
                    "Owner baseline approved",
                    false,
                    ownerCalibrating.view().statusHash(),
                    "promote-owner-version",
                    audit(9, "promote-owner-version")
            );

            assertTrue(ownerReady.view().canGateRewrites());
            assertEquals(
                    StyleProfileState.DEPRECATED,
                    service.getVersion(
                            created.profile().profileId(),
                            generated.view().profileVersion().versionId()
                    ).state()
            );
            assertThrows(StyleLifecycleConflictException.class, () ->
                    service.requireRewriteGate(
                            created.profile().profileId(),
                            generated.view().profileVersion().versionId()
                    )
            );
            assertThrows(StyleStatusPreconditionException.class, () ->
                    service.transition(
                            created.profile().profileId(),
                            ownerVersion.view().profileVersion().versionId(),
                            StyleProfileState.DEPRECATED,
                            "Stale attempt",
                            false,
                            ownerCalibrating.view().statusHash(),
                            "stale-transition",
                            audit(10, "stale-transition")
                    )
            );
        }

        try (SqliteDatabase database = SqliteDatabase.open(path)) {
            assertEquals(1, count(database, "style_profiles"));
            assertEquals(2, count(database, "style_profile_versions"));
            assertEquals(7, count(database, "style_profile_lifecycle_events"));
            SQLException profileUpdate = assertThrows(SQLException.class, () ->
                    database.write(connection -> {
                        connection.createStatement().executeUpdate(
                                "UPDATE style_profiles SET created_by = 'tampered'"
                        );
                        return null;
                    })
            );
            assertTrue(profileUpdate.getMessage().contains("immutable"));
            SQLException eventDelete = assertThrows(SQLException.class, () ->
                    database.write(connection -> {
                        connection.createStatement().executeUpdate(
                                "DELETE FROM style_profile_lifecycle_events"
                        );
                        return null;
                    })
            );
            assertTrue(eventDelete.getMessage().contains("append-only"));
        }
    }

    private static StyleProfileVersionContent content(
            RevisionManifest revision,
            StyleProfileScope scope,
            StyleCorpusSourceKind kind
    ) {
        StyleMaskingLexicon lexicon = StyleMaskingLexicon.empty();
        StyleFeatureSet features = new StyleFeatureAnalyzer().extract(
                revision.liveBlocks(),
                lexicon,
                StyleFeatureContract.defaults(lexicon.vocabularyHash())
        );
        StyleWindowConfiguration windows = StyleWindowConfiguration.defaults();
        List<BigDecimal> referenceDistances = java.util.Collections.nCopies(
                30, new BigDecimal("0.1")
        );
        StyleCalibrationProfile calibration = new StyleCalibrationProfile(
                dev.storyblock.style.StyleModule.CALIBRATION_SCHEMA_VERSION,
                features.sourceHash(),
                features.contract().contractHash(),
                windows.configurationHash(),
                List.of(new StyleStratumCalibration(
                        StyleStratum.narration(),
                        30,
                        dev.storyblock.style.StyleFeatureChannel.requiredChannels().stream()
                                .sorted(java.util.Comparator.comparing(Enum::ordinal))
                                .map(channel -> StyleChannelCalibration.fromDistances(
                                        channel, referenceDistances
                                )).toList()
                ))
        );
        return new StyleProfileVersionContent(
                scope,
                List.of(new StyleCorpusSource(
                        "corpus-" + kind.canonicalName(),
                        features.sourceHash(),
                        kind,
                        "Test corpus",
                        "owner-test-license",
                        "test-owner"
                )),
                features,
                windows,
                calibration.canonicalValue()
        );
    }

    private static AuditContext audit(int seconds, String request) {
        return new AuditContext(
                "req_" + request, "style-owner", null, NOW.plusSeconds(seconds)
        );
    }

    private static long count(SqliteDatabase database, String table) throws SQLException {
        if (!List.of(
                "style_profiles",
                "style_profile_versions",
                "style_profile_lifecycle_events"
        ).contains(table)) {
            throw new IllegalArgumentException("Unexpected style table " + table);
        }
        return database.readOnly(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                result.next();
                return result.getLong(1);
            }
        });
    }
}
