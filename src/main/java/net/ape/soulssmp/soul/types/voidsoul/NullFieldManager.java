package net.ape.soulssmp.soul.types.voidsoul;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NullFieldManager {

    private static class FieldData {
        final Location center;
        final double radius;
        final long expiresAt;

        FieldData(Location center, double radius, long expiresAt) {
            this.center = center;
            this.radius = radius;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<UUID, FieldData> activeFields = new HashMap<>();
    private final Map<UUID, Integer> fieldHitCounts = new HashMap<>();

    public void createField(Player caster, Location center, double radius, int durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        activeFields.put(caster.getUniqueId(), new FieldData(center, radius, expiresAt));
    }

    public UUID getFieldOwnerAt(Location location) {
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, FieldData> entry : activeFields.entrySet()) {
            FieldData data = entry.getValue();

            if (now > data.expiresAt) continue;
            if (!data.center.getWorld().equals(location.getWorld())) continue;

            if (data.center.distance(location) <= data.radius) {
                return entry.getKey();
            }
        }

        return null;
    }

    public void removeField(Player caster) {
        activeFields.remove(caster.getUniqueId());
    }

    /**
     * Registers a hit landed on this target while inside a Null Field.
     * Returns the new count.
     */
    public int registerFieldHit(LivingEntity target) {
        return fieldHitCounts.merge(target.getUniqueId(), 1, Integer::sum);
    }

    public void clearFieldHits(LivingEntity target) {
        fieldHitCounts.remove(target.getUniqueId());
    }
}