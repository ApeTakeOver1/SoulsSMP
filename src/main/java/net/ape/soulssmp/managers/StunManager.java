package net.ape.soulssmp.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Locks a stunned player's position in place by cancelling PlayerMoveEvent,
 * rather than approximating a stop with a Slowness effect (which only
 * reduces speed multiplicatively and never fully halts movement, and does
 * nothing to block jumping).
 */
public class StunManager implements Listener {

    private final Map<UUID, Long> stunnedUntil = new HashMap<>();

    public void stun(Player target, long durationTicks) {
        stunnedUntil.put(target.getUniqueId(), System.currentTimeMillis() + (durationTicks * 50L));
    }

    public boolean isStunned(Player player) {
        Long until = stunnedUntil.get(player.getUniqueId());
        if (until == null) return false;

        if (System.currentTimeMillis() > until) {
            stunnedUntil.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        boolean positionChanged = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
        if (positionChanged && isStunned(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        stunnedUntil.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        stunnedUntil.remove(event.getPlayer().getUniqueId());
    }
}
