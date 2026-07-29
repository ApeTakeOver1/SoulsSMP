package net.ape.soulssmp.managers.bloodsoul;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared "leashed to caster" system used by both Curse's Puppet effect
 * and Hemorrhage's bound state at 3/5 stacks. If the target strays past
 * the radius, RestraintTask pulls them back.
 * dragFully=true (Puppet): snaps them close to the caster.
 * dragFully=false (Hemorrhage bind): just stops them at the boundary.
 */
public class RestraintManager {

    public static class RestraintData {
        public final UUID casterId;
        public final double radius;
        public final boolean dragFully;
        public final long expiresAt;

        public RestraintData(UUID casterId, double radius, boolean dragFully, long expiresAt) {
            this.casterId = casterId;
            this.radius = radius;
            this.dragFully = dragFully;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<UUID, RestraintData> restraints = new HashMap<>();

    public void addRestraint(UUID targetId, UUID casterId, double radius, boolean dragFully, int durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        restraints.put(targetId, new RestraintData(casterId, radius, dragFully, expiresAt));
    }

    public RestraintData getRestraint(UUID targetId) {
        RestraintData data = restraints.get(targetId);
        if (data == null) return null;

        if (System.currentTimeMillis() > data.expiresAt) {
            restraints.remove(targetId);
            return null;
        }

        return data;
    }

    public boolean isRestrained(UUID targetId) {
        return getRestraint(targetId) != null;
    }

    public void removeRestraint(UUID targetId) {
        restraints.remove(targetId);
    }

    public Map<UUID, RestraintData> getAll() {
        return restraints;
    }
}