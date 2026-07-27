package net.ape.soulssmp.soul.types.bloodsoul;

import net.ape.soulssmp.SoulsSMP;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Continuous particle confirmation for debuffs with no natural vanilla
 * visual - Silence has zero built-in indicator at all, and Restriction's
 * Slowness/no-jump is easy to miss. Without this, a player genuinely
 * cannot tell these effects are active.
 */
public class BloodDebuffVisualTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (SoulsSMP.getInstance().getSilenceManager().isSilenced(player)) {
                player.getWorld().spawnParticle(
                        Particle.DUST, player.getLocation().add(0, 2.2, 0), 2, 0.2, 0.1, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(120, 0, 150), 1.0f)
                );
            }

            if (SoulsSMP.getInstance().getRestrictionManager().isRestricted(player)) {
                player.getWorld().spawnParticle(
                        Particle.DUST, player.getLocation().add(0, 0.1, 0), 4, 0.3, 0.05, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(150, 30, 30), 1.0f)
                );
            }
        }
    }
}