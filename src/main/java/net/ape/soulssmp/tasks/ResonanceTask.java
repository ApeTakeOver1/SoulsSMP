package net.ape.soulssmp.tasks;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Drains the Echo Soul's Noise Meter once per second when out of combat.
 * Mirrors ManaTask's role: owns the repeating loop, delegates the actual
 * state change to ResonanceManager.
 */
public class ResonanceTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);
            if (data.getSoul() != SoulType.ECHO) continue;

            SoulsSMP.getInstance().getResonanceManager().tickDrain(player);
        }
    }
}
