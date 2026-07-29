package net.ape.soulssmp.listeners;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.soul.SoulDefinition;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Sneak + F triggers whichever Ultimate matches the player's current Soul.
 * Replaces NullFieldTriggerListener now that there's more than one Soul.
 */
public class UltimateTriggerListener implements Listener {

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().isSneaking()) return;

        event.setCancelled(true);

        var data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data.getSoul() == null) return;

        SoulDefinition def = SoulsSMP.getInstance().getSoulRegistry().get(data.getSoul());
        if (def == null || def.getUltimate() == null) return;

        SoulsSMP.getInstance().getAbilityManager().useAbility(event.getPlayer(), def.getUltimate());
    }
}