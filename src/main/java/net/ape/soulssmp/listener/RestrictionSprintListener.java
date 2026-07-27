package net.ape.soulssmp.listener;

import net.ape.soulssmp.SoulsSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSprintEvent;

public class RestrictionSprintListener implements Listener {

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        if (event.isSprinting()
                && SoulsSMP.getInstance().getRestrictionManager().isRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}