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

    private final Map<UUID, MarkData> marks = new HashMap<>();

    public void applyMark(LivingEntity target, UUID markedBy, int durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        marks.put(target.getUniqueId(), new MarkData(markedBy, expiresAt));
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
        marks.remove(target.getUniqueId());
    }
}