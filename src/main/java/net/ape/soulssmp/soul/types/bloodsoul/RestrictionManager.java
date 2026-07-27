package net.ape.soulssmp.soul.types.bloodsoul;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Curse's "Restriction" effect: No Sprint, Slowness, Reduced Jump Height.
 * Slowness/Jump are real potion effects applied directly by Curse.java -
 * this manager only tracks the sprint-block flag, since there's no vanilla
 * potion effect for "can't sprint."
 */
public class RestrictionManager {

    private final Map<UUID, Long> restrictedUntil = new HashMap<>();

    public void applyRestriction(Player player, int durationSeconds) {
        restrictedUntil.put(player.getUniqueId(), System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    public boolean isRestricted(Player player) {
        Long until = restrictedUntil.get(player.getUniqueId());
        if (until == null) return false;

        if (System.currentTimeMillis() > until) {
            restrictedUntil.remove(player.getUniqueId());
            return false;
        }

        return true;
    }
}