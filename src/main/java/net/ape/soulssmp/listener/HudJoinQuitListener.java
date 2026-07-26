package net.ape.soulssmp.listener;

import net.ape.soulssmp.SoulsSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Creates the HUD boss bars when a player joins, removes them when they leave
 * (removing on quit prevents memory leaks from bars piling up forever).
 */
public class HudJoinQuitListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        SoulsSMP.getInstance().getHudManager().createBars(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SoulsSMP.getInstance().getHudManager().removeBars(event.getPlayer());
    }
}