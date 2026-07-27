package net.ape.soulssmp.soul.types.bloodsoul;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HemorrhageManager {

    public static class MarkData {
        public final UUID markedBy;
        public int stacks;
        public long expiresAt;

        public MarkData(UUID markedBy, long expiresAt) {
            this.markedBy = markedBy;
            this.stacks = 0;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<UUID, MarkData> marks = new HashMap<>(); // targetId -> data
    private final Map<UUID, UUID> markedTargetByCaster = new HashMap<>(); // casterId -> targetId

    public void applyMark(UUID targetId, UUID casterId, int durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        marks.put(targetId, new MarkData(casterId, expiresAt));
        markedTargetByCaster.put(casterId, targetId);
    }

    public boolean isMarkedBy(UUID targetId, UUID casterId) {
        MarkData data = marks.get(targetId);
        if (data == null) return false;

        if (System.currentTimeMillis() > data.expiresAt) {
            marks.remove(targetId);
            return false;
        }

        return data.markedBy.equals(casterId);
    }

    public int registerHit(UUID targetId, UUID casterId) {
        MarkData data = marks.get(targetId);
        if (data == null || !data.markedBy.equals(casterId)) return -1;

        if (System.currentTimeMillis() > data.expiresAt) {
            marks.remove(targetId);
            return -1;
        }

        if (data.stacks < 5) {
            data.stacks++;
        }
        return data.stacks;
    }

    public void clearMark(UUID targetId) {
        MarkData data = marks.remove(targetId);
        if (data != null) {
            markedTargetByCaster.remove(data.markedBy);
        }
    }

    /**
     * Returns [stacks, secondsRemaining] for the caster's currently marked
     * target, or null if they have no active mark. Used by the HUD to show
     * a live "X/5" counter instead of a plain cooldown bar.
     */
    public int[] getCurrentMarkStatus(UUID casterId) {
        UUID targetId = markedTargetByCaster.get(casterId);
        if (targetId == null) return null;

        MarkData data = marks.get(targetId);
        if (data == null) {
            markedTargetByCaster.remove(casterId);
            return null;
        }

        long remainingMillis = data.expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            marks.remove(targetId);
            markedTargetByCaster.remove(casterId);
            return null;
        }

        return new int[]{data.stacks, (int) (remainingMillis / 1000) + 1};
    }
}