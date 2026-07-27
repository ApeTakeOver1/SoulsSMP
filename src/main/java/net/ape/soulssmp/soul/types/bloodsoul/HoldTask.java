package net.ape.soulssmp.soul.types.bloodsoul;

import net.ape.soulssmp.SoulsSMP;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HoldTask extends BukkitRunnable {

    private int tickCounter = 0;

    @Override
    public void run() {
        tickCounter++;
        boolean dealDamageThisTick = tickCounter % 4 == 0; // every 1s (task runs every 5 ticks)

        HoldManager holdManager = SoulsSMP.getInstance().getHoldManager();
        Set<UUID> toRemove = new HashSet<>();

        for (var entry : new java.util.HashMap<>(holdManager.getAll()).entrySet()) {
            UUID targetId = entry.getKey();
            HoldManager.HoldData data = entry.getValue();

            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                toRemove.add(targetId);
                continue;
            }

            if (System.currentTimeMillis() > data.expiresAt) {
                toRemove.add(targetId);
                continue;
            }

            target.teleport(data.anchor);

            target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);

            if (dealDamageThisTick) {
                target.damage(1.0);
            }
        }

        for (UUID id : toRemove) {
            holdManager.release(id);
        }
    }
}