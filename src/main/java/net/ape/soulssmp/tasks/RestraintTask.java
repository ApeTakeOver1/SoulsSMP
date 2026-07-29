package net.ape.soulssmp.tasks;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.managers.bloodsoul.RestraintManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RestraintTask extends BukkitRunnable {

    @Override
    public void run() {
        RestraintManager manager = SoulsSMP.getInstance().getRestraintManager();
        Set<UUID> toRemove = new HashSet<>();

        for (var entry : new java.util.HashMap<>(manager.getAll()).entrySet()) {
            UUID targetId = entry.getKey();
            RestraintManager.RestraintData data = entry.getValue();

            Player target = Bukkit.getPlayer(targetId);
            Player caster = Bukkit.getPlayer(data.casterId);

            if (target == null || caster == null || !target.isOnline() || !caster.isOnline()) {
                toRemove.add(targetId);
                continue;
            }

            if (System.currentTimeMillis() > data.expiresAt) {
                toRemove.add(targetId);
                continue;
            }

            double distance = target.getLocation().distance(caster.getLocation());
            if (distance <= data.radius) continue;

            if (data.dragFully) {
                Location pullTo = caster.getLocation().clone().add(caster.getLocation().getDirection().multiply(-1.5));
                target.teleport(pullTo);
            } else {
                Vector direction = target.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize();
                Location boundaryPoint = caster.getLocation().clone().add(direction.multiply(data.radius));
                boundaryPoint.setY(target.getLocation().getY());
                target.teleport(boundaryPoint);
            }
        }

        for (UUID id : toRemove) {
            manager.removeRestraint(id);
        }
    }
}