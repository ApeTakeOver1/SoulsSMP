package net.ape.soulssmp.listeners;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.abilities.blood.active.Curse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSprintEvent;

public class RestrictionSprintListener implements Listener {

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        Curse curse = SoulsSMP.getInstance().getAbilityManager().getAs(AbilityType.CURSE, Curse.class);

        if (event.isSprinting() && curse.isRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}