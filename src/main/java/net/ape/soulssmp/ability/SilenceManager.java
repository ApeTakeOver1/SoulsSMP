package net.ape.soulssmp.ability;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared "Silence" status used by both Curse and Hemorrhage's rupture.
 * While silenced: abilities cost more mana, mana regen is slower,
 * and ability activation is delayed (handled in AbilityManager).
 */
public class SilenceManager {

    private static final double COST_MULTIPLIER = 1.5;
    private static final double MANA_GAIN_MULTIPLIER = 0.25;
    private static final long ACTIVATION_DELAY_TICKS = 20L; // 1 second

    private final Map<UUID, Long> silencedUntil = new HashMap<>();

    public void applySilence(Player player, int durationSeconds) {
        silencedUntil.put(player.getUniqueId(), System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    public boolean isSilenced(Player player) {
        Long until = silencedUntil.get(player.getUniqueId());
        if (until == null) return false;

        if (System.currentTimeMillis() > until) {
            silencedUntil.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    public double getCostMultiplier() {
        return COST_MULTIPLIER;
    }

    public double getManaGainMultiplier() {
        return MANA_GAIN_MULTIPLIER;
    }

    public long getActivationDelayTicks() {
        return ACTIVATION_DELAY_TICKS;
    }
}