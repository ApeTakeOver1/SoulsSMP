package net.ape.soulssmp.abilities.voidsoul.passive;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.data.PlayerData;
import net.ape.soulssmp.api.SoulType;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles Void Soul's passive:
 * - Resistance while below 60% health
 * - Drains nearby enemy (Soul-bound) players' mana while at full health
 * Runs every 10 ticks (0.5s) for both effects.
 */
public class VoidPassiveTask extends BukkitRunnable {

    private static final double LOW_HEALTH_THRESHOLD = 0.60;
    private static final double DRAIN_RADIUS = 8.0;
    private static final int DRAIN_PER_TICK = 2;
    private static final int RESISTANCE_DURATION_TICKS = 15;
    private static final int RESISTANCE_AMPLIFIER = 1;

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);

                if (data.getSoul() != SoulType.VOID) {
                    continue;
                }

                handleLowHealthResistance(player);
                handleFullHealthDrain(player);
            } catch (Exception e) {
                SoulsSMP.getInstance().getLogger().warning(
                        "VoidPassiveTask error for " + player.getName() + ": " + e.getMessage()
                );
            }
        }
    }

    private void handleLowHealthResistance(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double healthPercent = player.getHealth() / maxHealth;

        if (healthPercent <= LOW_HEALTH_THRESHOLD) {
            boolean alreadyHad = player.hasPotionEffect(PotionEffectType.RESISTANCE);

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE,
                    RESISTANCE_DURATION_TICKS,
                    RESISTANCE_AMPLIFIER,
                    false,
                    true,
                    true
            ));

            if (!alreadyHad) {
                player.sendMessage("§8§lVoid §7» §5Your Soul shields you from harm.");
            }
        }
    }

    private void handleFullHealthDrain(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (player.getHealth() < maxHealth) {
            return;
        }

        for (Entity nearby : player.getNearbyEntities(DRAIN_RADIUS, DRAIN_RADIUS, DRAIN_RADIUS)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.getUniqueId().equals(player.getUniqueId())) continue;

            PlayerData targetData = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(target);
            if (targetData.getSoul() == null) continue;

            int currentMana = SoulsSMP.getInstance().getManaManager().getMana(target);
            if (currentMana <= 0) continue;

            SoulsSMP.getInstance().getManaManager().setMana(target, currentMana - DRAIN_PER_TICK);
            target.getWorld().spawnParticle(
                    org.bukkit.Particle.SMOKE,
                    target.getLocation().add(0, 1.2, 0),
                    2, 0.1, 0.1, 0.1, 0.01
            );
        }
    }
}