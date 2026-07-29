package net.ape.soulssmp.managers.echosoul;

import net.ape.soulssmp.abilities.echo.EchoTier;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the Echo Soul's Noise Meter (Resonance), 0-100.
 * Combat-transient like Hemorrhage stacks / Abyss Mark - NOT persisted to
 * PlayerData or disk. Resets to 0 on join/soul (re)assignment.
 *
 * PLACEHOLDER TUNING VALUES - flagged for easy retuning:
 *   - OUT_OF_COMBAT_GRACE_SECONDS = 5
 *   - DRAIN_PER_SECOND = 2.0
 */
public class ResonanceManager {

    public static final double MAX_RESONANCE = 100.0;
    private static final long OUT_OF_COMBAT_GRACE_MILLIS = 5000L; // placeholder
    private static final double DRAIN_PER_SECOND = 2.0; // placeholder

    private final Map<UUID, Double> resonance = new HashMap<>();
    private final Map<UUID, Long> lastCombatActionAt = new HashMap<>();

    public double getResonance(Player player) {
        return resonance.getOrDefault(player.getUniqueId(), 0.0);
    }

    public EchoTier getTier(Player player) {
        return EchoTier.fromResonance(getResonance(player));
    }

    public void setResonance(Player player, double amount) {
        double clamped = Math.max(0, Math.min(MAX_RESONANCE, amount));
        resonance.put(player.getUniqueId(), clamped);
    }

    /**
     * Adds resonance and marks this as a combat action (resets the
     * out-of-combat drain grace timer). Use for damage dealt/taken,
     * nearby explosions, and (later) ability use/hits.
     */
    public void addResonance(Player player, double amount) {
        setResonance(player, getResonance(player) + amount);
        markCombatAction(player);
    }

    public void markCombatAction(Player player) {
        lastCombatActionAt.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Spends resonance for an ability cast. Returns false (and changes
     * nothing) if the player doesn't have enough banked. Creative mode
     * always succeeds without spending, matching Mana's Creative rules.
     */
    public boolean spendResonance(Player player, double cost) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;

        if (getResonance(player) < cost) {
            return false;
        }

        setResonance(player, getResonance(player) - cost);
        return true;
    }

    public boolean hasEnoughResonance(Player player, double cost) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        return getResonance(player) >= cost;
    }

    /**
     * Called once per drain tick (see ResonanceTask). Drains toward 0
     * only once OUT_OF_COMBAT_GRACE_MILLIS has passed since the last
     * combat action. No-op in Creative (mirrors Mana having no relevant
     * concept there - Echo abilities are free in Creative regardless).
     */
    public void tickDrain(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        double current = getResonance(player);
        if (current <= 0) return;

        Long lastAction = lastCombatActionAt.get(player.getUniqueId());
        if (lastAction != null && System.currentTimeMillis() - lastAction < OUT_OF_COMBAT_GRACE_MILLIS) {
            return; // still "in combat" - meter holds
        }

        setResonance(player, current - DRAIN_PER_SECOND);
    }

    public void clear(Player player) {
        resonance.remove(player.getUniqueId());
        lastCombatActionAt.remove(player.getUniqueId());
    }
}
