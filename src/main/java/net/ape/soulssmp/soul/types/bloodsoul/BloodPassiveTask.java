package net.ape.soulssmp.soul.types.bloodsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.player.PlayerData;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BloodPassiveTask extends BukkitRunnable {

    private static final double LOW_HEALTH_THRESHOLD = 0.40;
    private static final double SENSE_THRESHOLD = 0.25;
    private static final double SENSE_RADIUS = 10.0; // shrunk from 15 - this IS the real detection range

    private static final NamespacedKey ATTACK_SPEED_KEY =
            new NamespacedKey(SoulsSMP.getInstance(), "blood_pact_attack_speed");

    private static final Set<UUID> pactActive = new HashSet<>();

    private int tickCounter = 0;

    @Override
    public void run() {
        tickCounter++;
        boolean playHeartbeatThisTick = tickCounter % 2 == 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);

            if (data.getSoul() != SoulType.BLOOD) {
                if (pactActive.contains(player.getUniqueId())) {
                    removePact(player);
                }
                continue;
            }

            try {
                handleBloodPact(player);
                handleBloodSense(player, playHeartbeatThisTick);
            } catch (Exception e) {
                SoulsSMP.getInstance().getLogger().warning(
                        "BloodPassiveTask error for " + player.getName() + ": " + e.getMessage()
                );
            }
        }
    }

    private void handleBloodPact(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double healthPercent = player.getHealth() / maxHealth;

        if (healthPercent <= LOW_HEALTH_THRESHOLD) {
            applyPact(player);
        } else {
            removePact(player);
        }
    }

    private void applyPact(Player player) {
        boolean wasActive = pactActive.contains(player.getUniqueId());
        pactActive.add(player.getUniqueId());

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, 1, false, true, true));

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null && attackSpeed.getModifier(ATTACK_SPEED_KEY) == null) {
            attackSpeed.addModifier(new AttributeModifier(
                    ATTACK_SPEED_KEY, 0.5, AttributeModifier.Operation.ADD_NUMBER
            ));
        }

        if (!wasActive) {
            player.sendMessage("§4§lBlood §7» §cYour Blood Pact awakens.");
        }
    }

    private void removePact(Player player) {
        boolean wasActive = pactActive.remove(player.getUniqueId());

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) {
            AttributeModifier existing = attackSpeed.getModifier(ATTACK_SPEED_KEY);
            if (existing != null) {
                attackSpeed.removeModifier(existing);
            }
        }

        if (wasActive) {
            player.sendMessage("§4§lBlood §7» §7Your Blood Pact fades.");
        }
    }

    public static boolean isPactActive(Player player) {
        return pactActive.contains(player.getUniqueId());
    }

    private void handleBloodSense(Player observer, boolean playHeartbeat) {
        BloodSenseManager senseManager = SoulsSMP.getInstance().getBloodSenseManager();

        // STEP 1: re-check every target we're CURRENTLY sensing, even if
        // they're no longer nearby right now - this is what actually turns
        // the glow off when they leave range or heal up.
        for (UUID targetId : senseManager.getCurrentlySensedTargets(observer.getUniqueId())) {
            Player target = Bukkit.getPlayer(targetId);

            if (target == null || !target.isOnline()) {
                senseManager.stopSensing(observer.getUniqueId(), targetId);
                continue;
            }

            double distance = target.getLocation().distance(observer.getLocation());
            double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            double healthPercent = target.getHealth() / maxHealth;

            boolean stillQualifies = distance <= SENSE_RADIUS && healthPercent <= SENSE_THRESHOLD;

            if (!stillQualifies) {
                senseManager.stopSensing(observer.getUniqueId(), targetId);
                continue;
            }

            target.getWorld().spawnParticle(
                    Particle.DUST, target.getLocation().add(0, 1, 0),
                    3, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f)
            );

            if (playHeartbeat) {
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.5f);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 1.5f);
            }
        }

        // STEP 2: scan for NEW qualifying targets not already being tracked.
        for (Entity nearby : observer.getNearbyEntities(SENSE_RADIUS, SENSE_RADIUS, SENSE_RADIUS)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.getUniqueId().equals(observer.getUniqueId())) continue;

            double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            double healthPercent = target.getHealth() / maxHealth;

            if (healthPercent <= SENSE_THRESHOLD) {
                senseManager.startSensing(observer.getUniqueId(), target.getUniqueId());
            }
        }

        if (senseManager.isObserverSensingAnything(observer.getUniqueId())) {
            drawDetectionRing(observer);
        }
    }

    /**
     * Denser ring: smaller radius (10 blocks) and particles every 3 degrees
     * instead of every 10, for a noticeably thicker/more visible circle.
     */
    private void drawDetectionRing(Player observer) {
        Location center = observer.getLocation();

        for (int i = 0; i < 360; i += 3) {
            double rad = Math.toRadians(i);
            double x = center.getX() + SENSE_RADIUS * Math.cos(rad);
            double z = center.getZ() + SENSE_RADIUS * Math.sin(rad);
            Location edge = new Location(center.getWorld(), x, center.getY() + 0.2, z);

            center.getWorld().spawnParticle(
                    Particle.DUST, edge, 2, 0, 0.15, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(220, 0, 0), 1.3f)
            );
        }
    }
}