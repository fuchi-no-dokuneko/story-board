package dev.storyblock.style;



final class StyleProfileVersionViewCanGateRewritesAction {
    static boolean canGateRewrites(StyleProfileVersionView self)  {
        return self.state() == StyleProfileState.READY
                && self.approvedBy() != null
                && self.approvedAt() != null
                && self.profileVersion().content().hasGateCalibration()
                && (!self.profileVersion().content().containsGeneratedText()
                        || self.lifecycle().stream().anyMatch(event ->
                                event.toState() == StyleProfileState.READY
                                        && event.generatedPromotionConfirmed()
                        ));
    }
}
