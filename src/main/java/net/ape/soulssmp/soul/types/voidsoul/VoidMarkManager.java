package net.ape.soulssmp.soul.types.voidsoul;

import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoidMarkManager {

    private static class MarkData {
        final UUID markedBy;
        int hitCount;
        long expiresAt;

        MarkData(UUID markedBy, long expiresAt) {
            this.markedBy = markedBy;
            this.hitCount = 0;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<UUID, MarkData> marks = new HashMap<>(); // targetId -> data
    private final Map<UUID, UUID> markedTargetByCaster = new HashMap<>(); // casterId -> targetId

    public void applyMark(LivingEntity target, UUID markedBy, int durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        marks.put(target.getUniqueId(), new MarkData(markedBy, expiresAt));
        markedTargetByCaster.put(markedBy, target.getUniqueId());
    }

    public boolean isMarkedBy(LivingEntity target, UUID attacker) {
        MarkData data = marks.get(target.getUniqueId());
        if (data == null) return false;

        if (System.currentTimeMillis() > data.expiresAt) {
            marks.remove(target.getUniqueId());
            return false;
        }

        return data.markedBy.equals(attacker);
    }

    public int registerHit(LivingEntity target, UUID attacker) {
        MarkData data = marks.get(target.getUniqueId());
        if (data == null || !data.markedBy.equals(attacker)) return -1;

        if (System.currentTimeMillis() > data.expiresAt) {
            marks.remove(target.getUniqueId());
            return -1;
        }

        data.hitCount++;
        return data.hitCount;
    }

    public void clearMark(LivingEntity target) {
        MarkData data = marks.remove(target.getUniqueId());
        if (data != null) {
            markedTargetByCaster.remove(data.markedBy);
        }
    }

    /**
     * Returns how many seconds are left on the mark this caster currently
     * has active on their target, or 0 if they have no active mark.
     * Used by the HUD to show mark duration instead of a cooldown.
     */
    public int getRemainingMarkSeconds(UUID casterId) {
        UUID targetId = markedTargetByCaster.get(casterId);
        if (targetId == null) return 0;

        MarkData data = marks.get(targetId);
        if (data == null) {
            markedTargetByCaster.remove(casterId);
            return 0;
        }

        long remainingMillis = data.expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            marks.remove(targetId);
            markedTargetByCaster.remove(casterId);
            return 0;
        }

        return (int) (remainingMillis / 1000) + 1;
    }
}