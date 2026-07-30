package net.ape.soulssmp.tasks;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.abilities.blood.active.Curse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spigot-API-only replacement for PlayerJumpEvent (which is Paper-exclusive
 * and wasn't available in this project's dependency). Watches each
 * restricted player's ground state every tick; the instant they leave the
 * ground with upward velocity while restricted, their vertical velocity
 * gets zeroed out, effectively preventing the jump.
 */
public class RestrictionJumpTask extends BukkitRunnable {

    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();

    @Override
    public void run() {
        Curse curse = SoulsSMP.getInstance().getAbilityManager().getAs(AbilityType.CURSE, Curse.class);

        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean jumpLocked = curse.isJumpLocked(player);
            boolean onGroundNow = player.isOnGround();
            Boolean onGroundBefore = wasOnGround.get(player.getUniqueId());

            if (jumpLocked && onGroundBefore != null && onGroundBefore && !onGroundNow) {
                Vector velocity = player.getVelocity();
                if (velocity.getY() > 0) {
                    velocity.setY(0);
                    player.setVelocity(velocity);
                }
            }

            wasOnGround.put(player.getUniqueId(), onGroundNow);
        }
    }
}