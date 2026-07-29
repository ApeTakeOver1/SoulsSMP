package net.ape.soulssmp.managers;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the two boss bars every player sees: one for Mana, one for the
 * Null Field ultimate's cooldown/ready state.
 */
public class HudManager {

    private final Map<UUID, BossBar> manaBars = new HashMap<>();
    private final Map<UUID, BossBar> ultimateBars = new HashMap<>();

    private static class ActionBarOverride {
        final String message;
        final long expiresAt;

        ActionBarOverride(String message, long expiresAt) {
            this.message = message;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<UUID, ActionBarOverride> actionBarOverrides = new HashMap<>();

    /**
     * Briefly replaces the normal HUD action bar with a one-off message.
     * Needed because HudTask redraws the action bar every 4 ticks, which
     * would otherwise instantly overwrite a plain Player#sendActionBar call.
     */
    public void showActionBarOverride(Player player, String message, long durationMillis) {
        actionBarOverrides.put(player.getUniqueId(), new ActionBarOverride(message, System.currentTimeMillis() + durationMillis));
    }

    public String getActionBarOverride(UUID playerId) {
        ActionBarOverride override = actionBarOverrides.get(playerId);
        if (override == null) return null;

        if (System.currentTimeMillis() > override.expiresAt) {
            actionBarOverrides.remove(playerId);
            return null;
        }

        return override.message;
    }

    public void createBars(Player player) {
        removeBars(player); // clean up any old ones first (handles /reload safely)

        BossBar manaBar = Bukkit.createBossBar("§d§lMana", BarColor.PURPLE, BarStyle.SOLID);
        manaBar.addPlayer(player);
        manaBar.setProgress(0);
        manaBars.put(player.getUniqueId(), manaBar);

        BossBar ultimateBar = Bukkit.createBossBar("§f§lNull Field", BarColor.WHITE, BarStyle.SOLID);
        ultimateBar.addPlayer(player);
        ultimateBar.setProgress(1.0);
        ultimateBars.put(player.getUniqueId(), ultimateBar);
    }

    public void removeBars(Player player) {
        BossBar manaBar = manaBars.remove(player.getUniqueId());
        if (manaBar != null) {
            manaBar.removeAll();
        }

        BossBar ultimateBar = ultimateBars.remove(player.getUniqueId());
        if (ultimateBar != null) {
            ultimateBar.removeAll();
        }

        actionBarOverrides.remove(player.getUniqueId());
    }

    public BossBar getManaBar(Player player) {
        return manaBars.get(player.getUniqueId());
    }

    public BossBar getUltimateBar(Player player) {
        return ultimateBars.get(player.getUniqueId());
    }
}