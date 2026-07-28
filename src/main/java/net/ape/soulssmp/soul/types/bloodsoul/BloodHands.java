package net.ape.soulssmp.soul.types.bloodsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BloodHands extends Ability {

    public static final double PUNCTURE_HP_THRESHOLD = 10.0;

    private static final double HP_COST = 10.0;
    private static final int LIFESTEAL_BOOST_SECONDS = 10;

    private static final double SONIC_CLAP_RADIUS = 15.0;
    private static final int SONIC_CLAP_DURATION_SECONDS = 15;

    private static final double CONTROL_TRIGGER_RANGE = 5.0;
    private static final int CONTROL_HOLD_SECONDS = 10;

    private static final double NEEDLE_RANGE = 300.0;
    private static final double NEEDLE_HIT_TOLERANCE = 0.6;
    private static final double NEEDLE_DAMAGE = 1.0;

    private final Random random = new Random();

    public BloodHands() {
        super(AbilityType.BLOOD_HANDS, "Blood Hands", 0, 90, SoulType.BLOOD);
    }

    @Override
    public boolean execute(Player player) {
        if (player.getHealth() <= PUNCTURE_HP_THRESHOLD) {
            return executePuncture(player);
        }
        return executeBloodHands(player);
    }

    private boolean executeBloodHands(Player player) {
        boolean creative = player.getGameMode() == GameMode.CREATIVE;

        if (!creative) {
            if (player.getHealth() <= HP_COST + 0.5) {
                player.sendMessage("§4§lBlood §7» §cNot enough health to sacrifice for Blood Hands.");
                return false;
            }
            player.setHealth(player.getHealth() - HP_COST);
        }

        SoulsSMP.getInstance().getBloodCombatListener()
                .applyLifestealBoost(player.getUniqueId(), LIFESTEAL_BOOST_SECONDS);

        player.sendMessage("§4§lBlood §7» §cBlood Hands unleashed.");

        boolean controlEligible = hasNearbyTarget(player, CONTROL_TRIGGER_RANGE);
        boolean rollControl = controlEligible && random.nextBoolean();

        if (rollControl) {
            control(player);
        } else {
            sonicClap(player);
        }

        return true;
    }

    private boolean hasNearbyTarget(Player player, double range) {
        for (var entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player target && !target.getUniqueId().equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    private void sonicClap(Player player) {
        Location center = player.getLocation();

        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.6f);
        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.8f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 4, 1, 0.5, 1, 0);

        flashRadiusRing(center, SONIC_CLAP_RADIUS);

        for (var entity : center.getWorld().getNearbyEntities(center, SONIC_CLAP_RADIUS, SONIC_CLAP_RADIUS, SONIC_CLAP_RADIUS)) {
            if (!(entity instanceof Player target) || target.getUniqueId().equals(player.getUniqueId())) continue;

            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, SONIC_CLAP_DURATION_SECONDS * 20, 0, false, true, true));
            SoulsSMP.getInstance().getSilenceManager().applySilence(target, SONIC_CLAP_DURATION_SECONDS);
            target.sendMessage("§4§lBlood §7» §cSonic Clap blinded you.");
        }

        player.sendMessage("§4§lBlood §7» §fSonic Clap");
    }

    private void flashRadiusRing(Location center, double radius) {
        for (int pulse = 0; pulse < 3; pulse++) {
            long delay = pulse * 3L;
            double currentRadius = radius * ((pulse + 1) / 3.0);

            Bukkit.getScheduler().runTaskLater(SoulsSMP.getInstance(), () -> {
                for (int i = 0; i < 360; i += 5) {
                    double rad = Math.toRadians(i);
                    double x = center.getX() + currentRadius * Math.cos(rad);
                    double z = center.getZ() + currentRadius * Math.sin(rad);
                    Location edge = new Location(center.getWorld(), x, center.getY() + 0.1, z);
                    center.getWorld().spawnParticle(
                            Particle.DUST, edge, 2, 0, 0.3, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.4f)
                    );
                }
            }, delay);
        }
    }

    private void control(Player player) {
        List<LivingEntity> targets = new ArrayList<>();
        for (var entity : player.getNearbyEntities(CONTROL_TRIGGER_RANGE, CONTROL_TRIGGER_RANGE, CONTROL_TRIGGER_RANGE)) {
            if (entity instanceof Player target && !target.getUniqueId().equals(player.getUniqueId())) {
                targets.add(target);
            }
            if (targets.size() >= 2) break;
        }

        Location front = player.getLocation();
        var forward = front.getDirection().setY(0).normalize();
        var side = new org.bukkit.util.Vector(-forward.getZ(), 0, forward.getX());

        int index = 0;
        for (LivingEntity target : targets) {
            double sideOffset = index == 0 ? -0.8 : 0.8;
            Location anchor = front.clone().add(forward.clone().multiply(2.5)).add(side.clone().multiply(sideOffset)).add(0, 1.2, 0);

            SoulsSMP.getInstance().getHoldManager().hold(target.getUniqueId(), anchor, CONTROL_HOLD_SECONDS);

            target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, anchor, 25, 0.3, 0.5, 0.3, 0.02);
            target.getWorld().playSound(anchor, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 0.6f);

            if (target instanceof Player targetPlayer) {
                targetPlayer.sendMessage("§4§lBlood §7» §cYou're held in the Blood Hand's grasp.");
            }

            index++;
        }

        player.sendMessage("§4§lBlood §7» §fControl");
    }

    private boolean executePuncture(Player player) {
        LivingEntity target = getTargetEasy(player, NEEDLE_RANGE, NEEDLE_HIT_TOLERANCE);
        if (target == null) {
            player.sendMessage("§4§lBlood §7» §7Puncture found no target.");
            return false;
        }

        int heartsRemaining = (int) Math.ceil(player.getHealth() / 2.0);
        heartsRemaining = Math.max(0, Math.min(5, heartsRemaining));
        int needleCount = 10 + 2 * (5 - heartsRemaining);

        double chargeDurationSeconds = 2.0 + Math.max(0, (needleCount - 10) / 10.0) * 3.0;
        chargeDurationSeconds = Math.max(2.0, Math.min(5.0, chargeDurationSeconds));
        long chargeTicks = Math.round(chargeDurationSeconds * 20);

        player.sendMessage("§4§lBlood §7» §fPuncture §7— gathering " + needleCount + " needles ("
                + String.format("%.1f", chargeDurationSeconds) + "s)...");

        chargeFormation(player, needleCount, chargeTicks, () -> releaseNeedleBurst(player, target, needleCount));
        return true;
    }

    /**
     * Builds a growing particle cluster behind the player over the charge
     * window. Fix applied here: the loop variable "step" cannot be used
     * directly inside the lambda passed to runTaskLater (Java requires
     * captured variables to be effectively final, and a for-loop counter
     * is never final). Copying it into "currentStep" right before the
     * lambda solves this - each iteration gets its own fixed copy.
     */
    private void chargeFormation(Player player, int needleCount, long chargeTicks, Runnable onComplete) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.7f, 0.6f);

        int steps = 10;
        long stepInterval = Math.max(1L, chargeTicks / steps);

        for (int step = 1; step <= steps; step++) {
            final int currentStep = step;
            long delay = currentStep * stepInterval;
            int particlesThisStep = Math.max(2, (needleCount / steps) * currentStep);

            Bukkit.getScheduler().runTaskLater(SoulsSMP.getInstance(), () -> {
                Location behind = player.getLocation().clone()
                        .subtract(player.getLocation().getDirection().multiply(1.2)).add(0, 1, 0);

                player.getWorld().spawnParticle(
                        Particle.DUST, behind, particlesThisStep, 0.2, 0.25, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.3f)
                );

                if (currentStep % 3 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.8f);
                }
            }, delay);
        }

        Bukkit.getScheduler().runTaskLater(SoulsSMP.getInstance(), onComplete, chargeTicks);
    }

    private void releaseNeedleBurst(Player player, LivingEntity target, int needleCount) {
        if (target.isDead() || !target.isValid()) return;

        Location behind = player.getLocation().clone()
                .subtract(player.getLocation().getDirection().multiply(1.2)).add(0, 1, 0);
        Location targetPoint = target.getLocation().add(0, 1, 0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.6f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.8f, 1.8f);

        for (int i = 0; i < needleCount; i++) {
            double spreadX = (random.nextDouble() - 0.5) * 0.6;
            double spreadZ = (random.nextDouble() - 0.5) * 0.6;
            Location origin = behind.clone().add(spreadX, 0, spreadZ);

            drawNeedleLine(origin, targetPoint);
        }

        double totalDamage = needleCount * NEEDLE_DAMAGE;
        double newHealth = Math.max(0, target.getHealth() - totalDamage);
        target.setHealth(newHealth);

        target.getWorld().spawnParticle(
                Particle.DUST, targetPoint, 20, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(210, 0, 0), 1.5f)
        );
    }

    private void drawNeedleLine(Location from, Location to) {
        var direction = to.clone().subtract(from).toVector().normalize();
        double distance = from.distance(to);

        for (double d = 0; d < distance; d += 0.2) {
            Location point = from.clone().add(direction.clone().multiply(d));
            from.getWorld().spawnParticle(
                    Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(210, 0, 0), 1.3f)
            );
        }
    }

    private LivingEntity getTargetEasy(Player player, double range, double raySize) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                raySize,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (result == null || !(result.getHitEntity() instanceof LivingEntity)) return null;
        return (LivingEntity) result.getHitEntity();
    }
}