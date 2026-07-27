package net.ape.soulssmp.soul.types.bloodsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Blood Soul's Ultimate. No mana cost - instead costs HP directly and runs
 * on cooldown alone. Grants a temporary lifesteal boost, then randomly
 * fires one of 3 effects: Sonic Clap, Puncture, or Control.
 */
public class BloodHands extends Ability {

    private static final double HP_COST = 10.0;
    private static final int LIFESTEAL_BOOST_SECONDS = 10;
    private static final double SONIC_CLAP_RADIUS = 8.0;
    private static final int SONIC_CLAP_DURATION_SECONDS = 15;
    private static final double PUNCTURE_RANGE = 20.0;
    private static final int PUNCTURE_BLEED_SECONDS = 15;
    private static final double CONTROL_RANGE = 10.0;
    private static final int CONTROL_HOLD_SECONDS = 10;

    private final Random random = new Random();

    public BloodHands() {
        super(AbilityType.BLOOD_HANDS, "Blood Hands", 0, 90, SoulType.BLOOD);
    }

    @Override
    public boolean execute(Player player) {
        if (player.getHealth() <= HP_COST + 0.5) {
            player.sendMessage("§4§lBlood §7» §cNot enough health to sacrifice for Blood Hands.");
            return false;
        }

        player.setHealth(player.getHealth() - HP_COST);
        SoulsSMP.getInstance().getBloodCombatListener()
                .applyLifestealBoost(player.getUniqueId(), LIFESTEAL_BOOST_SECONDS);

        player.sendMessage("§4§lBlood §7» §cBlood Hands unleashed.");

        int roll = random.nextInt(3);
        switch (roll) {
            case 0 -> sonicClap(player);
            case 1 -> puncture(player);
            case 2 -> control(player);
        }

        return true;
    }

    private void sonicClap(Player player) {
        Location center = player.getLocation();

        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.7f);
        player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, center, 60, 3, 2, 3, 0.05);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 3, 1, 0.5, 1, 0);

        for (var entity : center.getWorld().getNearbyEntities(center, SONIC_CLAP_RADIUS, SONIC_CLAP_RADIUS, SONIC_CLAP_RADIUS)) {
            if (!(entity instanceof Player target) || target.getUniqueId().equals(player.getUniqueId())) continue;

            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, SONIC_CLAP_DURATION_SECONDS * 20, 0, false, true, true));
            SoulsSMP.getInstance().getSilenceManager().applySilence(target, SONIC_CLAP_DURATION_SECONDS);
            target.sendMessage("§4§lBlood §7» §cSonic Clap silenced and blinded you.");
        }

        player.sendMessage("§4§lBlood §7» §fSonic Clap");
    }

    private void puncture(Player player) {
        LivingEntity target = getTarget(player, PUNCTURE_RANGE);
        if (target == null) {
            player.sendMessage("§4§lBlood §7» §7Puncture found no target.");
            return;
        }

        // Blood arrow visual - a fast traveling particle line from caster to target
        Location start = player.getEyeLocation();
        Location end = target.getLocation().add(0, 1, 0);
        var direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        for (double d = 0; d < distance; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(
                    Particle.DUST, point, 3, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.3f)
            );
        }
        player.getWorld().playSound(end, Sound.ENTITY_ARROW_HIT_PLAYER, 0.8f, 0.7f);

        target.damage(4.0, player);
        applyBleed(target, player);

        player.sendMessage("§4§lBlood §7» §fPuncture");
    }

    private void applyBleed(LivingEntity target, Player caster) {
        int totalTicks = PUNCTURE_BLEED_SECONDS;
        for (int i = 1; i <= totalTicks; i++) {
            final int tick = i;
            Bukkit.getScheduler().runTaskLater(SoulsSMP.getInstance(), () -> {
                if (target.isDead() || !target.isValid()) return;

                target.damage(1.0);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 0.3f, 0.6f);
                target.getWorld().spawnParticle(
                        Particle.DUST, target.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f)
                );

                if (tick == totalTicks) {
                    ruptureBurst(target, caster);
                }
            }, tick * 20L);
        }
    }

    private void ruptureBurst(LivingEntity target, Player caster) {
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 40, 1, 1, 1,
                0, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.6f));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);
        target.damage(3.0, caster);

        if (target instanceof Player targetPlayer) {
            var direction = targetPlayer.getLocation().getDirection().multiply(-1);
            targetPlayer.setVelocity(direction.multiply(1.2).setY(0.4));
            SoulsSMP.getInstance().getSilenceManager().applySilence(targetPlayer, 5);
        }
    }

    private void control(Player player) {
        List<LivingEntity> targets = new ArrayList<>();
        for (var entity : player.getNearbyEntities(CONTROL_RANGE, CONTROL_RANGE, CONTROL_RANGE)) {
            if (entity instanceof Player target && !target.getUniqueId().equals(player.getUniqueId())) {
                targets.add(target);
            }
            if (targets.size() >= 2) break;
        }

        if (targets.isEmpty()) {
            player.sendMessage("§4§lBlood §7» §7Control found no targets.");
            return;
        }

        int offset = 2;
        for (LivingEntity target : targets) {
            Location anchor = player.getLocation().clone().add(offset, 2, 0);
            SoulsSMP.getInstance().getHoldManager().hold(target.getUniqueId(), anchor, CONTROL_HOLD_SECONDS);

            target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, anchor, 25, 0.3, 0.5, 0.3, 0.02);
            target.getWorld().playSound(anchor, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 0.6f);

            if (target instanceof Player targetPlayer) {
                targetPlayer.sendMessage("§4§lBlood §7» §cYou're held in the Blood Hand's grasp.");
            }

            offset *= -1; // alternate sides for the second target
        }

        player.sendMessage("§4§lBlood §7» §fControl");
    }

    private LivingEntity getTarget(Player player, double range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (result == null || !(result.getHitEntity() instanceof LivingEntity)) return null;
        return (LivingEntity) result.getHitEntity();
    }
}