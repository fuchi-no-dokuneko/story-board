package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.StorageException;
import dev.storyblock.style.CreateStyleProfileCommand;
import dev.storyblock.style.CreateStyleProfileVersionCommand;
import dev.storyblock.style.MissingStyleProfileException;
import dev.storyblock.style.MissingStyleProfileVersionException;
import dev.storyblock.style.StyleLifecycleConflictException;
import dev.storyblock.style.StyleLifecycleEvent;
import dev.storyblock.style.StyleProfile;
import dev.storyblock.style.StyleProfileSaveResult;
import dev.storyblock.style.StyleProfileState;
import dev.storyblock.style.StyleProfileVersion;
import dev.storyblock.style.StyleProfileVersionSaveResult;
import dev.storyblock.style.StyleProfileVersionView;
import dev.storyblock.style.StyleStatusPreconditionException;
import dev.storyblock.style.TransitionStyleProfileVersionCommand;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class SqliteStyleProfileStore {
    private SqliteStyleProfileStore() {
    }

    static StyleProfileSaveResult createProfile(
            Connection connection,
            CreateStyleProfileCommand command
    ) throws SQLException {
        Ids.NovelId novelId = command.profile().scope().novelId();
        Optional<Mutation> prior = findMutation(
                connection, novelId, command.idempotencyKey()
        );
        if (prior.isPresent()) {
            Mutation replay = requireReplay(
                    prior.get(), MutationKind.CREATE_PROFILE, command.requestHash(),
                    command.idempotencyKey()
            );
            return new StyleProfileSaveResult(
                    getProfile(connection, replay.profileId()), true
            );
        }

        StyleProfile profile = command.profile();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO style_profiles(
                    profile_id, novel_id, profile_json, resource_hash, created_by, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, profile.profileId().value());
            statement.setString(2, novelId.value());
            statement.setString(3, CanonicalJson.string(profile.canonicalValue()));
            statement.setString(4, profile.resourceHash());
            statement.setString(5, profile.createdBy());
            statement.setString(6, profile.createdAt().toString());
            statement.executeUpdate();
        }
        insertMutation(
                connection,
                novelId,
                command.idempotencyKey(),
                MutationKind.CREATE_PROFILE,
                command.requestHash(),
                profile.profileId(),
                null,
                null,
                command.auditContext()
        );
        return new StyleProfileSaveResult(profile, false);
    }

    static StyleProfile getProfile(
            Connection connection,
            Ids.StyleProfileId profileId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT novel_id, profile_json, resource_hash, created_by, created_at
                FROM style_profiles
                WHERE profile_id = ?
                """)) {
            statement.setString(1, profileId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingStyleProfileException(profileId);
                }
                StyleProfile profile = StyleProfile.fromCanonical(parseObject(
                        result.getString("profile_json"), "style profile"
                ));
                if (!profile.profileId().equals(profileId)
                        || !profile.scope().novelId().value().equals(
                                result.getString("novel_id")
                        )
                        || !profile.resourceHash().equals(result.getString("resource_hash"))
                        || !profile.createdBy().equals(result.getString("created_by"))
                        || !profile.createdAt().toString().equals(result.getString("created_at"))) {
                    throw new StorageException("Stored style profile integrity check failed");
                }
                return profile;
            }
        }
    }

    static StyleProfileVersionSaveResult createVersion(
            Connection connection,
            CreateStyleProfileVersionCommand command
    ) throws SQLException {
        StyleProfile profile = getProfile(connection, command.profileId());
        Ids.NovelId novelId = profile.scope().novelId();
        Optional<Mutation> prior = findMutation(
                connection, novelId, command.idempotencyKey()
        );
        if (prior.isPresent()) {
            Mutation replay = requireReplay(
                    prior.get(), MutationKind.CREATE_VERSION, command.requestHash(),
                    command.idempotencyKey()
            );
            return new StyleProfileVersionSaveResult(
                    getVersion(connection, replay.profileId(), requireVersionId(replay)), true
            );
        }
        if (!profile.resourceHash().equals(command.expectedProfileHash())) {
            throw new StyleStatusPreconditionException(profile.resourceHash());
        }
        if (!profile.scope().equals(command.content().scope())) {
            throw new StyleLifecycleConflictException(
                    "Style profile version scope must exactly match its immutable profile scope"
            );
        }

        int number = nextVersionNumber(connection, profile.profileId());
        StyleProfileVersion version = new StyleProfileVersion(
                command.versionId(),
                profile.profileId(),
                number,
                command.content(),
                command.auditContext().actorId(),
                command.auditContext().occurredAt()
        );
        insertVersion(connection, version);
        StyleLifecycleEvent initial = StyleLifecycleEvent.initial(
                command.initialEventId(),
                profile.profileId(),
                version.versionId(),
                command.auditContext()
        );
        insertLifecycleEvent(connection, initial);
        insertMutation(
                connection,
                novelId,
                command.idempotencyKey(),
                MutationKind.CREATE_VERSION,
                command.requestHash(),
                profile.profileId(),
                version.versionId(),
                initial.eventId(),
                command.auditContext()
        );
        return new StyleProfileVersionSaveResult(
                StyleProfileVersionView.of(version, List.of(initial)), false
        );
    }

    static StyleProfileVersionView getVersion(
            Connection connection,
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId
    ) throws SQLException {
        StyleProfileVersion version;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT version_json, version_hash, created_by, created_at
                FROM style_profile_versions
                WHERE profile_id = ? AND version_id = ?
                """)) {
            statement.setString(1, profileId.value());
            statement.setString(2, versionId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingStyleProfileVersionException(profileId, versionId);
                }
                version = StyleProfileVersion.fromCanonical(parseObject(
                        result.getString("version_json"), "style profile version"
                ));
                if (!version.profileId().equals(profileId)
                        || !version.versionId().equals(versionId)
                        || !version.versionHash().equals(result.getString("version_hash"))
                        || !version.createdBy().equals(result.getString("created_by"))
                        || !version.createdAt().toString().equals(result.getString("created_at"))) {
                    throw new StorageException(
                            "Stored style profile version integrity check failed"
                    );
                }
            }
        }
        return StyleProfileVersionView.of(
                version, lifecycle(connection, profileId, versionId)
        );
    }

    static StyleProfileVersionSaveResult transition(
            Connection connection,
            TransitionStyleProfileVersionCommand command
    ) throws SQLException {
        StyleProfile profile = getProfile(connection, command.profileId());
        Ids.NovelId novelId = profile.scope().novelId();
        Optional<Mutation> prior = findMutation(
                connection, novelId, command.idempotencyKey()
        );
        if (prior.isPresent()) {
            Mutation replay = requireReplay(
                    prior.get(), MutationKind.TRANSITION, command.requestHash(),
                    command.idempotencyKey()
            );
            return new StyleProfileVersionSaveResult(
                    getVersion(connection, replay.profileId(), requireVersionId(replay)), true
            );
        }

        StyleProfileVersionView current = getVersion(
                connection, command.profileId(), command.versionId()
        );
        if (!current.statusHash().equals(command.expectedStatusHash())) {
            throw new StyleStatusPreconditionException(current.statusHash());
        }
        if (!current.state().canTransitionTo(command.targetState())) {
            throw new StyleLifecycleConflictException(
                    "Style profile lifecycle must follow DRAFT -> CALIBRATING -> READY "
                            + "-> DEPRECATED"
            );
        }
        if (command.targetState() == StyleProfileState.READY
                && current.profileVersion().content().containsGeneratedText()
                && !command.confirmGeneratedCorpusPromotion()) {
            throw new StyleLifecycleConflictException(
                    "Generated or mixed corpus requires explicit promotion confirmation"
            );
        }

        if (command.targetState() == StyleProfileState.READY) {
            deprecatePriorReady(connection, current, command.auditContext());
        }
        StyleLifecycleEvent event = new StyleLifecycleEvent(
                command.eventId(),
                command.profileId(),
                command.versionId(),
                current.lifecycle().size() + 1,
                current.state(),
                command.targetState(),
                command.reason(),
                command.targetState() == StyleProfileState.READY
                        && command.confirmGeneratedCorpusPromotion(),
                command.auditContext(),
                command.auditContext().occurredAt()
        );
        insertLifecycleEvent(connection, event);
        insertMutation(
                connection,
                novelId,
                command.idempotencyKey(),
                MutationKind.TRANSITION,
                command.requestHash(),
                command.profileId(),
                command.versionId(),
                event.eventId(),
                command.auditContext()
        );
        List<StyleLifecycleEvent> lifecycle = new ArrayList<>(current.lifecycle());
        lifecycle.add(event);
        return new StyleProfileVersionSaveResult(
                StyleProfileVersionView.of(current.profileVersion(), lifecycle), false
        );
    }

    private static void deprecatePriorReady(
            Connection connection,
            StyleProfileVersionView promoted,
            AuditContext auditContext
    ) throws SQLException {
        List<Ids.StyleProfileVersionId> ready = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT version.version_id
                FROM style_profile_versions AS version
                JOIN style_profile_lifecycle_events AS event
                  ON event.version_id = version.version_id
                WHERE version.profile_id = ?
                  AND version.version_id <> ?
                  AND event.sequence = (
                      SELECT MAX(current.sequence)
                      FROM style_profile_lifecycle_events AS current
                      WHERE current.version_id = version.version_id
                  )
                  AND event.to_state = 'ready'
                ORDER BY version.version
                """)) {
            statement.setString(1, promoted.profileVersion().profileId().value());
            statement.setString(2, promoted.profileVersion().versionId().value());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ready.add(new Ids.StyleProfileVersionId(result.getString(1)));
                }
            }
        }
        if (ready.size() > 1) {
            throw new StorageException("Multiple active READY style versions were detected");
        }
        if (ready.isEmpty()) {
            return;
        }
        StyleProfileVersionView prior = getVersion(
                connection, promoted.profileVersion().profileId(), ready.getFirst()
        );
        StyleLifecycleEvent deprecated = new StyleLifecycleEvent(
                Ids.StyleLifecycleEventId.create(),
                prior.profileVersion().profileId(),
                prior.profileVersion().versionId(),
                prior.lifecycle().size() + 1,
                StyleProfileState.READY,
                StyleProfileState.DEPRECATED,
                "Superseded by READY version "
                        + promoted.profileVersion().versionId().value(),
                false,
                auditContext,
                auditContext.occurredAt()
        );
        insertLifecycleEvent(connection, deprecated);
    }

    private static void insertVersion(
            Connection connection,
            StyleProfileVersion version
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO style_profile_versions(
                    version_id, profile_id, version, version_json, version_hash,
                    created_by, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, version.versionId().value());
            statement.setString(2, version.profileId().value());
            statement.setInt(3, version.version());
            statement.setString(4, CanonicalJson.string(version.canonicalValue()));
            statement.setString(5, version.versionHash());
            statement.setString(6, version.createdBy());
            statement.setString(7, version.createdAt().toString());
            statement.executeUpdate();
        }
    }

    private static void insertLifecycleEvent(
            Connection connection,
            StyleLifecycleEvent event
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO style_profile_lifecycle_events(
                    event_id, profile_id, version_id, sequence, from_state, to_state,
                    event_json, request_id, actor_id, actor_key_id, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, event.eventId().value());
            statement.setString(2, event.profileId().value());
            statement.setString(3, event.versionId().value());
            statement.setInt(4, event.sequence());
            if (event.fromState() == null) {
                statement.setNull(5, java.sql.Types.VARCHAR);
            } else {
                statement.setString(5, event.fromState().canonicalName());
            }
            statement.setString(6, event.toState().canonicalName());
            statement.setString(7, CanonicalJson.string(event.canonicalValue()));
            statement.setString(8, event.auditContext().requestId());
            statement.setString(9, event.auditContext().actorId());
            if (event.auditContext().actorKeyId() == null) {
                statement.setNull(10, java.sql.Types.VARCHAR);
            } else {
                statement.setString(10, event.auditContext().actorKeyId().value());
            }
            statement.setString(11, event.occurredAt().toString());
            statement.executeUpdate();
        }
    }

    private static List<StyleLifecycleEvent> lifecycle(
            Connection connection,
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId
    ) throws SQLException {
        List<StyleLifecycleEvent> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT event_json, from_state, to_state, request_id, actor_id,
                       actor_key_id, occurred_at
                FROM style_profile_lifecycle_events
                WHERE profile_id = ? AND version_id = ?
                ORDER BY sequence
                """)) {
            statement.setString(1, profileId.value());
            statement.setString(2, versionId.value());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    StyleLifecycleEvent event = StyleLifecycleEvent.fromCanonical(
                            parseObject(result.getString("event_json"), "style lifecycle event")
                    );
                    String from = result.getString("from_state");
                    String actorKey = result.getString("actor_key_id");
                    if (!event.profileId().equals(profileId)
                            || !event.versionId().equals(versionId)
                            || !java.util.Objects.equals(
                                    from,
                                    event.fromState() == null
                                            ? null : event.fromState().canonicalName()
                            )
                            || !event.toState().canonicalName().equals(
                                    result.getString("to_state")
                            )
                            || !event.auditContext().requestId().equals(
                                    result.getString("request_id")
                            )
                            || !event.auditContext().actorId().equals(
                                    result.getString("actor_id")
                            )
                            || !java.util.Objects.equals(
                                    actorKey,
                                    event.auditContext().actorKeyId() == null
                                            ? null : event.auditContext().actorKeyId().value()
                            )
                            || !event.occurredAt().toString().equals(
                                    result.getString("occurred_at")
                            )) {
                        throw new StorageException(
                                "Stored style lifecycle event integrity check failed"
                        );
                    }
                    events.add(event);
                }
            }
        }
        if (events.isEmpty()) {
            throw new StorageException("Stored style profile version has no lifecycle");
        }
        return List.copyOf(events);
    }

    private static int nextVersionNumber(
            Connection connection,
            Ids.StyleProfileId profileId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COALESCE(MAX(version), 0) + 1
                FROM style_profile_versions
                WHERE profile_id = ?
                """)) {
            statement.setString(1, profileId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new StorageException("Could not allocate style profile version");
                }
                return result.getInt(1);
            }
        }
    }

    private static Optional<Mutation> findMutation(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT mutation_kind, request_hash, profile_id, version_id, event_id
                FROM style_profile_mutations
                WHERE novel_id = ? AND idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                String version = result.getString("version_id");
                String event = result.getString("event_id");
                return Optional.of(new Mutation(
                        MutationKind.fromCanonical(result.getString("mutation_kind")),
                        result.getString("request_hash"),
                        new Ids.StyleProfileId(result.getString("profile_id")),
                        version == null ? null : new Ids.StyleProfileVersionId(version),
                        event == null ? null : new Ids.StyleLifecycleEventId(event)
                ));
            }
        }
    }

    private static Mutation requireReplay(
            Mutation mutation,
            MutationKind kind,
            String requestHash,
            String idempotencyKey
    ) {
        if (mutation.kind() != kind || !mutation.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    idempotencyKey, mutation.requestHash(), requestHash
            );
        }
        return mutation;
    }

    private static Ids.StyleProfileVersionId requireVersionId(Mutation mutation) {
        if (mutation.versionId() == null) {
            throw new StorageException("Style mutation is missing its version identity");
        }
        return mutation.versionId();
    }

    private static void insertMutation(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey,
            MutationKind kind,
            String requestHash,
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId,
            Ids.StyleLifecycleEventId eventId,
            AuditContext auditContext
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO style_profile_mutations(
                    novel_id, idempotency_key, mutation_kind, request_hash,
                    profile_id, version_id, event_id, request_id, actor_id,
                    actor_key_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            statement.setString(3, kind.canonicalName());
            statement.setString(4, requestHash);
            statement.setString(5, profileId.value());
            if (versionId == null) {
                statement.setNull(6, java.sql.Types.VARCHAR);
            } else {
                statement.setString(6, versionId.value());
            }
            if (eventId == null) {
                statement.setNull(7, java.sql.Types.VARCHAR);
            } else {
                statement.setString(7, eventId.value());
            }
            statement.setString(8, auditContext.requestId());
            statement.setString(9, auditContext.actorId());
            if (auditContext.actorKeyId() == null) {
                statement.setNull(10, java.sql.Types.VARCHAR);
            } else {
                statement.setString(10, auditContext.actorKeyId().value());
            }
            statement.setString(11, auditContext.occurredAt().toString());
            statement.executeUpdate();
        }
    }

    private static Map<String, Object> parseObject(String json, String path) {
        Object parsed = CanonicalJson.mapper().readValue(
                json.getBytes(StandardCharsets.UTF_8), Map.class
        );
        if (!(parsed instanceof Map<?, ?> raw)) {
            throw new StorageException("Stored " + path + " is not an object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new StorageException("Stored " + path + " has a non-string key");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private enum MutationKind {
        CREATE_PROFILE("create_profile"),
        CREATE_VERSION("create_version"),
        TRANSITION("transition");

        private final String canonicalName;

        MutationKind(String canonicalName) {
            this.canonicalName = canonicalName;
        }

        String canonicalName() {
            return canonicalName;
        }

        static MutationKind fromCanonical(String value) {
            for (MutationKind kind : values()) {
                if (kind.canonicalName.equals(value)) {
                    return kind;
                }
            }
            throw new StorageException("Unknown stored style mutation kind");
        }
    }

    private record Mutation(
            MutationKind kind,
            String requestHash,
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId,
            Ids.StyleLifecycleEventId eventId
    ) {
    }
}
