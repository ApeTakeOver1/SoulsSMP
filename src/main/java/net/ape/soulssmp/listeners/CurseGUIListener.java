package net.ape.soulssmp.listeners;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.abilities.blood.active.Curse;
import net.ape.soulssmp.gui.CurseGUI;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CurseGUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(CurseGUI.TITLE)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        Curse curse = SoulsSMP.getInstance().getAbilityManager().getAs(AbilityType.CURSE, Curse.class);

        LivingEntity target = curse.getPendingTarget(player.getUniqueId());
        if (target == null) {
            player.sendMessage("§4§lBlood §7» §7That curse has already faded.");
            player.closeInventory();
            return;
        }

        switch (event.getSlot()) {
            case 2 -> curse.applyRestriction(target, player);
            case 4 -> curse.applySilence(target, player);
            case 6 -> curse.applyPuppet(target, player);
            default -> {
                return;
            }
        }

        curse.clearPendingTarget(player.getUniqueId());
        player.closeInventory();
    }
}