package net.ape.soulssmp.soul.types.voidsoul;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which targets are currently "Null Field affected" per caster.
 * Being affected lasts the same 45 seconds as the Weakness/Blindness debuff,
 * and upgrades that caster's Abyss Mark combo bonuses against that target.
 */
public class NullFieldManager {

    private static class AffectedData {
        long expiresAt;

        AffectedData(long expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    // casterId -> (targetId -> data)
    private final Map<UUID, Map<UUID, AffectedData>> affected = new HashMap<>();

    public void markAffected(UUID casterId, UUID targetId, int durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        affected.computeIfAbsent(casterId, k -> new HashMap<>()).put(targetId, new AffectedData(expiresAt));
    }

    public boolean isAffected(UUID casterId, UUID targetId) {
        Map<UUID, AffectedData> targets = affected.get(casterId);
        if (targets == null) return false;

        AffectedData data = targets.get(targetId);
        if (data == null) return false;

        if (System.currentTimeMillis() > data.expiresAt) {
            targets.remove(targetId);
            return false;
        }

        return true;
    }
}