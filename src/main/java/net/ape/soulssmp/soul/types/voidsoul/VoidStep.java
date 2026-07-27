package net.ape.soulssmp.soul.types.voidsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VoidStep extends Ability {

    private static final double DISTANCE = 6.0;
    private static final long ARM_WINDOW_MILLIS = 4000L;
    private static final long ARM_WINDOW_TICKS = 80L;
    private static final double HIT_RADIUS = 1.5;
    private static final double DAMAGE = 6.0;

    private final Map<UUID, Long> armedUntil = new HashMap<>();
    private final Map<UUID, Location> armedDestination = new HashMap<>();

    public VoidStep() {
        super(AbilityType.VOID_STEP, "Void Step", 20, 10, SoulType.VOID);
    }

    @Override
    public boolean execute(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long armExpiry = armedUntil.get(uuid);

        if (armExpiry != null && now <= armExpiry) {
            armedUntil.remove(uuid);
            Location destination = armedDestination.remove(uuid);
            if (destination == null) destination = player.getLocation();

            if (!SoulsSMP.getInstance().getManaManager().spendMana(player, getManaCost())) {
                player.sendMessage("§8§lVoid §7» §cNot enough mana to complete Void Step.");
                SoulsSMP.getInstance().getShadowCloneManager().removeClone(player);
                return false;
            }

            performTeleport(player, destination);
            return true;
        }

        Location destination = calculateDestination(player);
        armedUntil.put(uuid, now + ARM_WINDOW_MILLIS);
        armedDestination.put(uuid, destination);

        playArmVisual(player, destination);
        player.sendMessage("§8§lVoid §7» §5Void Step armed. Use it again to strike.");

        Bukkit.getScheduler().runTaskLater(SoulsSMP.getInstance(), () -> {
            Long stillArmed = armedUntil.get(uuid);
            if (stillArmed != null && stillArmed == (now + ARM_WINDOW_MILLIS)) {
                armedUntil.remove(uuid);
                armedDestination.remove(uuid);
                SoulsSMP.getInstance().getShadowCloneManager().removeClone(player);
            }
        }, ARM_WINDOW_TICKS);

        return false;
    }

    private Location calculateDestination(Player player) {
        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize();
        return start.clone().add(direction.clone().multiply(DISTANCE));
    }

    private void performTeleport(Player player, Location destination) {
        Location start = player.getLocation();
        Vector direction = destination.clone().subtract(start).toVector().normalize();
        double distance = start.distance(destination);

        Set<LivingEntity> alreadyHit = new HashSet<>();

        for (double d = 0; d < distance; d += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(d));

            player.getWorld().spawnParticle(
                    Particle.DUST, point, 4, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.fromRGB(10, 0, 20), 1.4f)
            );
            player.getWorld().spawnParticle(Particle.SMOKE, point, 2, 0.05, 0.05, 0.05, 0);

            for (var entity : player.getWorld().getNearbyEntities(point, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (living.getUniqueId().equals(player.getUniqueId())) continue;
                if (!alreadyHit.add(living)) continue;

                living.damage(DAMAGE, player);
                living.getWorld().spawnParticle(Particle.SWEEP_ATTACK, living.getLocation().add(0, 1, 0), 1);
            }
        }

        SoulsSMP.getInstance().getShadowCloneManager().removeClone(player);

        destination.setDirection(start.getDirection());
        player.teleport(destination);

        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, destination, 2, 0.3, 0.3, 0.3, 0);
        player.getWorld().spawnParticle(
                Particle.DUST, destination, 25, 0.4, 0.6, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(20, 0, 30), 1.6f)
        );
        player.getWorld().playSound(destination, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 1.7f);
        player.getWorld().playSound(destination, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.6f, 0.6f);
    }

    private void playArmVisual(Player player, Location destination) {
        SoulsSMP.getInstance().getShadowCloneManager().spawnClone(player, destination);

        player.getWorld().spawnParticle(Particle.SMOKE, destination.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.01);
        player.getWorld().spawnParticle(
                Particle.DUST, destination.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(15, 0, 25), 1.3f)
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.4f, 0.8f);
    }
}