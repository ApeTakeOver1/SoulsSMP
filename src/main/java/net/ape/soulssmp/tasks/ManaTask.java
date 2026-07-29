package net.ape.soulssmp.tasks;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.abilities.voidsoul.ultimate.NullField;
import net.ape.soulssmp.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ManaTask extends BukkitRunnable {

    private static final int MANA_PER_TICK = 5;
    private static final int NULL_FIELD_BOOSTED_MANA_PER_TICK = MANA_PER_TICK * 2;

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);

            if (data.getSoul() == null) continue;
            if (data.getMana() >= data.getMaxMana()) continue;

            if (SoulsSMP.getInstance().getSilenceManager().isSilenced(player)) {
                continue; // Silenced - no mana regeneration at all
            }

            int manaPerTick = SoulsSMP.getInstance().getAbilityManager().getAs(AbilityType.NULL_FIELD, NullField.class)
                    .isManaBoostActive(player.getUniqueId())
                    ? NULL_FIELD_BOOSTED_MANA_PER_TICK
                    : MANA_PER_TICK;

            int before = data.getMana();
            SoulsSMP.getInstance().getManaManager().addMana(player, manaPerTick);
            int after = data.getMana();

            if (after > before) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.4f);
            }
        }
    }
}