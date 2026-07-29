package net.ape.soulssmp.abilities.echo;

/**
 * The 5 bands of the Echo Soul's Noise Meter (Resonance), 0-100%.
 * minInclusive/maxExclusive define the band; PERFECT_ECHO's max is 100
 * inclusive (handled as a special case in fromResonance()).
 */
public enum EchoTier {

    DORMANT(0, 0, "§7Dormant"),
    ECHO(0, 25, "§fEcho"),
    DISTORTED_ECHO(25, 50, "§7Distorted Echo"),
    LOST_SIGNAL(50, 75, "§8Lost Signal"),
    PERFECT_ECHO(75, 100, "§8§lPerfect Echo");

    private final double minInclusive;
    private final double maxExclusive;
    private final String displayName;

    EchoTier(double minInclusive, double maxExclusive, String displayName) {
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
        this.displayName = displayName;
    }

    public double getMinInclusive() {
        return minInclusive;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * DORMANT only ever applies at exactly 0. Above 0 you're always at
     * least in the ECHO band (per the design doc: "0% permanent speed 2"
     * is the baseline, "0-25%: Echo" is the first real band).
     */
    public static EchoTier fromResonance(double resonance) {
        if (resonance <= 0) return DORMANT;
        if (resonance <= 25) return ECHO;
        if (resonance <= 50) return DISTORTED_ECHO;
        if (resonance <= 75) return LOST_SIGNAL;
        return PERFECT_ECHO;
    }
}
