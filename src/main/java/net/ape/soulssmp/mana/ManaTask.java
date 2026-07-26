package net.ape.soulssmp.mana;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Passive mana regeneration.
 * Runs every 5 seconds and adds 5 mana per tick — same average rate as
 * "1 mana per second," but shows up as a clean +5 jump instead of a slow crawl.
 * Caps at whatever the player's current max mana is.
 */
public class ManaTask extends BukkitRunnable {

    private static final int MANA_PER_TICK = 5;

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);

            if (data.getSoul() == null) continue; // no soul yet, nothing to regen
            if (data.getMana() >= data.getMaxMana()) continue; // already full

            int before = data.getMana();
            SoulsSMP.getInstance().getManaManager().addMana(player, MANA_PER_TICK);
            int after = data.getMana();

            if (after > before) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.4f);
            }
        }
    }
}