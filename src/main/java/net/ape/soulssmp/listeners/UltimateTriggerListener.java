package net.ape.soulssmp.listeners;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.api.SoulType;
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

        if (data.getSoul() == SoulType.VOID) {
            SoulsSMP.getInstance().getAbilityManager().useAbility(event.getPlayer(), AbilityType.NULL_FIELD);
        } else if (data.getSoul() == SoulType.BLOOD) {
            SoulsSMP.getInstance().getAbilityManager().useAbility(event.getPlayer(), AbilityType.BLOOD_HANDS);
        }
    }
}