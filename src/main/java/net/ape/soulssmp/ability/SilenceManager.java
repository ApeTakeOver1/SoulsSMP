package net.ape.soulssmp.ability;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Silence now means: abilities cost double mana, and you cannot regenerate
 * mana at all while silenced (handled by ManaTask skipping regen entirely).
 * Applying it always gives Darkness + a message, centralized here so every
 * call site (Curse, Sonic Clap, Rupture) gets consistent feedback.
 */
public class SilenceManager {

    private static final double COST_MULTIPLIER = 2.0; // doubles ability mana cost

    private final Map<UUID, Long> silencedUntil = new HashMap<>();

    public void applySilence(Player player, int durationSeconds) {
        silencedUntil.put(player.getUniqueId(), System.currentTimeMillis() + (durationSeconds * 1000L));

        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, durationSeconds * 20, 0, false, true, true));
        player.sendMessage("§5§lSilenced §7» §7You cannot regenerate mana and abilities cost double.");
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
}