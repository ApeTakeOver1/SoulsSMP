package net.ape.soulssmp.soul.types.bloodsoul;

import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds the locked target for a caster between "Curse channel finished"
 * and "player picked an effect from the GUI".
 */
public class CursePendingManager {

    private final Map<UUID, LivingEntity> pending = new HashMap<>();

    public void setPendingTarget(UUID casterId, LivingEntity target) {
        pending.put(casterId, target);
    }

    public LivingEntity getPendingTarget(UUID casterId) {
        return pending.get(casterId);
    }

    public void clearPending(UUID casterId) {
        pending.remove(casterId);
    }
}