package net.ape.soulssmp.listeners;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.data.PlayerData;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.abilities.blood.passive.BloodPassiveTask;
import net.ape.soulssmp.abilities.blood.active.Hemorrhage;
import org.bukkit.ChatColor;
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

    private static final double HEMORRHAGE_TETHER_RADIUS = 15.0;
    private static final int HEMORRHAGE_TETHER_DURATION_SECONDS = 30;
    private static final double RUPTURE_DAMAGE = 10.0;
    private static final long ACTION_BAR_FEEDBACK_MILLIS = 2500L;

    private static final String HEMO_PREFIX =
            ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Hemorrhage" + ChatColor.GRAY + " » " + ChatColor.RED;

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
        Hemorrhage hemorrhage = SoulsSMP.getInstance().getAbilityManager().getAs(AbilityType.HEMORRHAGE, Hemorrhage.class);
        if (!hemorrhage.isMarkedBy(target.getUniqueId(), attacker.getUniqueId())) return;

        int stacks = hemorrhage.registerHit(target.getUniqueId(), attacker.getUniqueId());
        if (stacks <= 0) return;

        switch (stacks) {
            case 1 -> attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);

            case 2 -> {
                if (target instanceof Player) {
                    target.setGlowing(true);
                }
                showHemorrhageFeedback(attacker, "Target glowing.");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.0f);
            }

            case 3 -> {
                if (target instanceof Player targetPlayer) {
                    SoulsSMP.getInstance().getRestraintManager()
                            .addRestraint(targetPlayer.getUniqueId(), attacker.getUniqueId(),
                                    HEMORRHAGE_TETHER_RADIUS, false, HEMORRHAGE_TETHER_DURATION_SECONDS);
                    targetPlayer.sendMessage("§4§lBlood §7» §cYou've been tethered by Hemorrhage.");
                }
                showHemorrhageFeedback(attacker, "Target tethered.");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.9f, 0.9f);
            }

            case 4 -> attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);

            default -> {
                hemorrhage.clearMark(target.getUniqueId());
                showHemorrhageFeedback(attacker, "Rupture!");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.0f);
                triggerRupture(target, attacker);
            }
        }
    }

    private void showHemorrhageFeedback(Player attacker, String message) {
        SoulsSMP.getInstance().getHudManager()
                .showActionBarOverride(attacker, HEMO_PREFIX + message, ACTION_BAR_FEEDBACK_MILLIS);
    }

    private void triggerRupture(LivingEntity target, Player attacker) {
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 40, 1, 1, 1, 0,
                new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.6f));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);

        double newHealth = Math.max(0, target.getHealth() - RUPTURE_DAMAGE);
        target.setHealth(newHealth);

        if (target instanceof Player targetPlayer) {
            var direction = targetPlayer.getLocation().getDirection().multiply(-1);
            targetPlayer.setVelocity(direction.multiply(1.3).setY(0.4));
            targetPlayer.setGlowing(false);
            targetPlayer.sendMessage("§4§lBlood §7» §cRupture — 5 hearts of true damage.");
        }
    }
}
