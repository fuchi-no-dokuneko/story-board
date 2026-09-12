package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import dev.storyblock.security.*;
import dev.storyblock.monitor.*;
import dev.storyblock.rewrite.policy.*;
import dev.storyblock.style.*;
import dev.storyblock.storage.*;
import java.util.Objects;

interface SqliteProfilePort extends StyleProfileStore, SqliteStoreContext {
  @Override
  default StyleProfileSaveResult createStyleProfile(CreateStyleProfileCommand command) {
    Objects.requireNonNull(command, "command");
    return context().write(connection -> SqliteProfileCreateProfile.createProfile(
        connection, command
    ));
  }

  @Override
  default StyleProfile getStyleProfile(Ids.StyleProfileId profileId) {
    Objects.requireNonNull(profileId, "profileId");
    return context().read(connection -> SqliteProfileGetProfile.getProfile(
        connection, profileId
    ));
  }

  @Override
  default StyleProfileVersionSaveResult createStyleProfileVersion(
      CreateStyleProfileVersionCommand command
  ) {
    Objects.requireNonNull(command, "command");
    return context().write(connection -> SqliteProfileCreateVersion.createVersion(
        connection, command
    ));
  }

  @Override
  default StyleProfileVersionView getStyleProfileVersion(
      Ids.StyleProfileId profileId,
      Ids.StyleProfileVersionId versionId
  ) {
    Objects.requireNonNull(profileId, "profileId");
    Objects.requireNonNull(versionId, "versionId");
    return context().read(connection -> SqliteProfileGetVersion.getVersion(
        connection, profileId, versionId
    ));
  }

  @Override
  default StyleProfileVersionSaveResult transitionStyleProfileVersion(
      TransitionStyleProfileVersionCommand command
  ) {
    Objects.requireNonNull(command, "command");
    return context().write(connection -> SqliteProfileTransition.transition(
        connection, command
    ));
  }
}
