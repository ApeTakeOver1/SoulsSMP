package net.ape.soulssmp.listener;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.AbilityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class NullFieldTriggerListener implements Listener {

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().isSneaking()) return;

        event.setCancelled(true); // stop it from actually swapping items
        SoulsSMP.getInstance().getAbilityManager().useAbility(event.getPlayer(), AbilityType.NULL_FIELD);
    }
}