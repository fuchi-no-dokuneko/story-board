package dev.storyblock.api.http;

import dev.storyblock.style.StyleProfileVersionView;

final class StyleProfileControllerVersionLocation {
    static String versionLocation(StyleProfileVersionView view) {
        return "/v1/style-profiles/"
                + view.profileVersion().profileId().value()
                + "/versions/"
                + view.profileVersion().versionId().value();
    }
}
