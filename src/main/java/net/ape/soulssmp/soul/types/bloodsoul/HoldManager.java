package net.ape.soulssmp.soul.types.bloodsoul;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Blood Hands' "Control" sub-effect: holds targets in a fixed spot for
 * 10 seconds, dealing periodic tick damage (like entity cramming), while
 * still allowing them to attack/act - only movement is locked.
 */
public class HoldManager {

    public static class HoldData {
        public final Location anchor;
        public final long expiresAt;

        public HoldData(Location anchor, long expiresAt) {
            this.anchor = anchor;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<UUID, HoldData> held = new HashMap<>();

    public void hold(UUID targetId, Location anchor, int durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        held.put(targetId, new HoldData(anchor, expiresAt));
    }

    public HoldData getHold(UUID targetId) {
        HoldData data = held.get(targetId);
        if (data == null) return null;

        if (System.currentTimeMillis() > data.expiresAt) {
            held.remove(targetId);
            return null;
        }

        return data;
    }

    public void release(UUID targetId) {
        held.remove(targetId);
    }

    public Map<UUID, HoldData> getAll() {
        return held;
    }
}