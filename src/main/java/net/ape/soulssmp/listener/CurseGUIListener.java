package net.ape.soulssmp.listener;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.soul.types.bloodsoul.Curse;
import net.ape.soulssmp.soul.types.bloodsoul.CurseGUI;
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

        LivingEntity target = SoulsSMP.getInstance().getCursePendingManager().getPendingTarget(player.getUniqueId());
        if (target == null) {
            player.sendMessage("§4§lBlood §7» §7That curse has already faded.");
            player.closeInventory();
            return;
        }

        Curse curse = (Curse) SoulsSMP.getInstance().getAbilityManager().getAbility(AbilityType.CURSE);

        switch (event.getSlot()) {
            case 2 -> curse.applyRestriction(target, player);
            case 4 -> curse.applySilence(target, player);
            case 6 -> curse.applyPuppet(target, player);
            default -> {
                return;
            }
        }

        SoulsSMP.getInstance().getCursePendingManager().clearPending(player.getUniqueId());
        player.closeInventory();
    }
}