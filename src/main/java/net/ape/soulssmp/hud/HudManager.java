package net.ape.soulssmp.hud;

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
    }

    public BossBar getManaBar(Player player) {
        return manaBars.get(player.getUniqueId());
    }

    public BossBar getUltimateBar(Player player) {
        return ultimateBars.get(player.getUniqueId());
    }
}