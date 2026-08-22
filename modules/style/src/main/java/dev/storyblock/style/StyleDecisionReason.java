package dev.storyblock.style;

public enum StyleDecisionReason {
    WITHIN_CALIBRATED_RANGE("within_calibrated_range"),
    INSUFFICIENT_CALIBRATION("insufficient_calibration"),
    TOKEN_CHANNEL_ONLY("token_channel_only"),
    MULTI_CHANNEL_Q95("multi_channel_q95"),
    SUSTAINED_MULTI_CHANNEL_Q99("sustained_multi_channel_q99"),
    INTENTIONAL_STYLE_SHIFT("intentional_style_shift");

    private final String canonicalName;

    StyleDecisionReason(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
