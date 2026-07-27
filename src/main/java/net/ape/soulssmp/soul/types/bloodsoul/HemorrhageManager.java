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

    private final Map<UUID, MarkData> marks = new HashMap<>();

    public void applyMark(UUID targetId, UUID casterId, int durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        marks.put(targetId, new MarkData(casterId, expiresAt));
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

    /**
     * Registers a hit, returns the new stack count (1-5), or -1 if not marked.
     */
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
        marks.remove(targetId);
    }
}