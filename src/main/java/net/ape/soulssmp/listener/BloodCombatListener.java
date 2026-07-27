package net.ape.soulssmp.listener;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.player.PlayerData;
import net.ape.soulssmp.soul.SoulType;
import net.ape.soulssmp.soul.types.bloodsoul.BloodPassiveTask;
import net.ape.soulssmp.soul.types.bloodsoul.HemorrhageManager;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BloodCombatListener implements Listener {

    private static final double NORMAL_LIFESTEAL_PERCENT = 0.10;
    private static final double BOOSTED_LIFESTEAL_PERCENT = 0.25;

    private final Map<UUID, Long> bloodHandsLifestealUntil = new HashMap<>();

    public void applyLifestealBoost(UUID playerId, int durationSeconds) {
        bloodHandsLifestealUntil.put(playerId, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    private boolean hasLifestealBoost(UUID playerId) {
        Long until = bloodHandsLifestealUntil.get(playerId);
        if (until == null) return false;

        if (System.currentTimeMillis() > until) {
            bloodHandsLifestealUntil.remove(playerId);
            return false;
        }

        return true;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        handleLifesteal(event, attacker);

        if (event.getEntity() instanceof LivingEntity target) {
            handleHemorrhageStacks(attacker, target);
        }
    }

    private void handleLifesteal(EntityDamageByEntityEvent event, Player attacker) {
        PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(attacker);
        if (data.getSoul() != SoulType.BLOOD) return;

        boolean boosted = hasLifestealBoost(attacker.getUniqueId());
        boolean pactActive = BloodPassiveTask.isPactActive(attacker);

        if (!boosted && !pactActive) return;

        double percent = boosted ? BOOSTED_LIFESTEAL_PERCENT : NORMAL_LIFESTEAL_PERCENT;
        double healAmount = event.getFinalDamage() * percent;
        double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(maxHealth, attacker.getHealth() + healAmount);

        attacker.setHealth(newHealth);
    }

    private void handleHemorrhageStacks(Player attacker, LivingEntity target) {
        HemorrhageManager hemorrhageManager = SoulsSMP.getInstance().getHemorrhageManager();
        if (!hemorrhageManager.isMarkedBy(target.getUniqueId(), attacker.getUniqueId())) return;

        int stacks = hemorrhageManager.registerHit(target.getUniqueId(), attacker.getUniqueId());
        if (stacks <= 0) return;

        switch (stacks) {
            case 1 -> attacker.sendMessage("§4§lHemorrhage §7» §c1/5");

            case 2 -> {
                if (target instanceof Player) {
                    target.setGlowing(true);
                }
                attacker.sendMessage("§4§lHemorrhage §7» §c2/5 §7- Target Marked");
            }

            case 3 -> {
                if (target instanceof Player targetPlayer) {
                    SoulsSMP.getInstance().getRestraintManager()
                            .addRestraint(targetPlayer.getUniqueId(), attacker.getUniqueId(), 10.0, false, 30);
                    targetPlayer.sendMessage("§4§lBlood §7» §cYou are bound - you cannot leave the radius.");
                }
                attacker.sendMessage("§4§lHemorrhage §7» §c3/5 §7- Target Tethered");
            }

            case 4 -> attacker.sendMessage("§4§lHemorrhage §7» §c4/5");

            default -> {
                // Clear the mark BEFORE dealing rupture damage to avoid the
                // recursive rupture loop from re-firing this event.
                hemorrhageManager.clearMark(target.getUniqueId());
                attacker.sendMessage("§4§lHemorrhage §7» §c5/5 §7- RUPTURE");
                triggerRupture(target, attacker);
            }
        }
    }

    private void triggerRupture(LivingEntity target, Player attacker) {
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 40, 1, 1, 1, 0,
                new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.6f));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);

        double newHealth = Math.max(0, target.getHealth() - 5.0);
        target.setHealth(newHealth);

        if (target instanceof Player targetPlayer) {
            var direction = targetPlayer.getLocation().getDirection().multiply(-1);
            targetPlayer.setVelocity(direction.multiply(1.3).setY(0.4));
            SoulsSMP.getInstance().getSilenceManager().applySilence(targetPlayer, 5);
            targetPlayer.setGlowing(false);
            targetPlayer.sendTitle("§c§lRUPTURE", "§7Silenced for 5 seconds", 5, 40, 10);
        }
    }
}