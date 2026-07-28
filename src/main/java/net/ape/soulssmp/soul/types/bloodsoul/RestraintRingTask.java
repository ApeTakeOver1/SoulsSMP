package net.ape.soulssmp.soul.types.bloodsoul;

import net.ape.soulssmp.SoulsSMP;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Continuously draws a ring around the CASTER for every active restraint
 * (Puppet and Hemorrhage's 3-stack tether both use RestraintManager) so
 * the boundary is always visible, same idea as Blood Sense's ring.
 */
public class RestraintRingTask extends BukkitRunnable {

    @Override
    public void run() {
        RestraintManager manager = SoulsSMP.getInstance().getRestraintManager();

        for (var entry : new java.util.HashMap<>(manager.getAll()).entrySet()) {
            RestraintManager.RestraintData data = entry.getValue();
            Player caster = Bukkit.getPlayer(data.casterId);
            if (caster == null || !caster.isOnline()) continue;

            drawRing(caster.getLocation(), data.radius);
        }
    }

    private void drawRing(Location center, double radius) {
        for (int i = 0; i < 360; i += 6) {
            double rad = Math.toRadians(i);
            double x = center.getX() + radius * Math.cos(rad);
            double z = center.getZ() + radius * Math.sin(rad);
            Location edge = new Location(center.getWorld(), x, center.getY() + 0.2, z);

            center.getWorld().spawnParticle(
                    Particle.DUST, edge, 1, 0, 0.1, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(140, 0, 30), 1.2f)
            );
        }
    }
}