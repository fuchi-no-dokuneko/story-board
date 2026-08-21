package dev.storyblock.style;

import dev.storyblock.domain.Ids;

public interface StyleProfileStore {
    StyleProfileSaveResult createStyleProfile(CreateStyleProfileCommand command);

    StyleProfile getStyleProfile(Ids.StyleProfileId profileId);

    StyleProfileVersionSaveResult createStyleProfileVersion(
            CreateStyleProfileVersionCommand command
    );

    StyleProfileVersionView getStyleProfileVersion(
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId
    );

    StyleProfileVersionSaveResult transitionStyleProfileVersion(
            TransitionStyleProfileVersionCommand command
    );
}
