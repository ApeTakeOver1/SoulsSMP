package net.ape.soulssmp.listener;

import net.ape.soulssmp.SoulsSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles loading player data on join and saving it on quit.
 * Runs at LOWEST priority on join so data is loaded before anything
 * else (HUD, etc.) tries to read it.
 */
public class PlayerDataListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        SoulsSMP.getInstance().getPlayerDataManager().loadPlayerData(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        SoulsSMP.getInstance().getPlayerDataManager().savePlayerData(event.getPlayer());
        SoulsSMP.getInstance().getPlayerDataManager().removePlayerData(event.getPlayer());
    }
}