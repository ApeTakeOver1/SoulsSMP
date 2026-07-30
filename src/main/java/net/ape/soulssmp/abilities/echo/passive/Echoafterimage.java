package net.ape.soulssmp.abilities.echo;

import net.ape.soulssmp.SoulsSMP;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

/**
 * Perfect Echo's attack afterimage. Best-effort approximation of "your
 * player model spawns in and attacks" - Bukkit has no API to spawn a real
 * copy of a player entity, so this dresses an ArmorStand with the
 * attacker's skin (via a player-head skull), worn armor, and held item,
 * then poses/moves it in a short lunge-and-swing toward the target before
 * despawning. It's a puppet wearing the attacker's look, not a true clone.
 */
public class EchoAfterimage {

    private static final int LUNGE_TICKS = 5;
    private static final int LINGER_TICKS_AFTER_HIT = 4;
    private static final double SPAWN_BEHIND_DISTANCE = 1.5;

    private EchoAfterimage() {
    }

    public static void spawn(Player attacker, LivingEntity target) {
        Location attackerLoc = attacker.getLocation();

        Vector towardTarget = target.getLocation().toVector().subtract(attackerLoc.toVector());
        towardTarget.setY(0);
        if (towardTarget.lengthSquared() < 0.0001) {
            towardTarget = attackerLoc.getDirection().setY(0);
        }
        towardTarget.normalize();

        Location spawnAt = attackerLoc.clone().subtract(towardTarget.clone().multiply(SPAWN_BEHIND_DISTANCE));
        spawnAt.setDirection(towardTarget);

        Location targetPoint = target.getLocation().clone();
        targetPoint.setDirection(towardTarget);

        ArmorStand stand = attacker.getWorld().spawn(spawnAt, ArmorStand.class, as -> {
            as.setVisible(true);
            as.setArms(true);
            as.setBasePlate(false);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setMarker(false);
            as.setSmall(false);
            as.setCollidable(false);
            as.setSilent(true);
        });

        dressLikeAttacker(stand, attacker);

        stand.getWorld().playSound(spawnAt, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.4f);

        Vector fixedDirection = towardTarget;
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!stand.isValid()) {
                    cancel();
                    return;
                }

                if (tick <= LUNGE_TICKS) {
                    double progress = (double) tick / LUNGE_TICKS;
                    Location step = spawnAt.clone().add(
                            targetPoint.clone().subtract(spawnAt).toVector().multiply(progress)
                    );
                    step.setDirection(fixedDirection);
                    stand.teleport(step);

                    // swing the right arm forward as it closes the distance
                    double swingDegrees = -60 + (140 * progress);
                    stand.setRightArmPose(new EulerAngle(Math.toRadians(swingDegrees), 0, 0));
                }

                if (tick == LUNGE_TICKS) {
                    stand.getWorld().spawnParticle(Particle.SWEEP_ATTACK, targetPoint.clone().add(0, 1.2, 0), 1);
                    stand.getWorld().playSound(targetPoint, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.6f, 1.3f);
                }

                if (tick >= LUNGE_TICKS + LINGER_TICKS_AFTER_HIT) {
                    stand.remove();
                    cancel();
                    return;
                }

                tick++;
            }
        }.runTaskTimer(SoulsSMP.getInstance(), 0L, 1L);
    }

    private static void dressLikeAttacker(ArmorStand stand, Player attacker) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(attacker);
            head.setItemMeta(meta);
        }

        EntityEquipment standEquipment = stand.getEquipment();
        EntityEquipment attackerEquipment = attacker.getEquipment();
        if (standEquipment == null || attackerEquipment == null) return;

        standEquipment.setHelmet(head);
        standEquipment.setChestplate(attackerEquipment.getChestplate());
        standEquipment.setLeggings(attackerEquipment.getLeggings());
        standEquipment.setBoots(attackerEquipment.getBoots());
        standEquipment.setItemInMainHand(attackerEquipment.getItemInMainHand());
    }
}