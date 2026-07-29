package net.ape.soulssmp.abilities.voidsoul.active;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.Ability;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.api.SoulType;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbyssMark extends Ability {

    private static final double RANGE = 30.0;
    private static final int MARK_DURATION_SECONDS = 10;
    private static final long ACTION_BAR_OVERRIDE_MILLIS = 1500L;

    // Mark/combo tracking (formerly VoidMarkManager) - AbyssMark owns this state;
    // VoidCombatListener and HudTask read it via SoulsSMP.getInstance().getAbyssMark().
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

    public AbyssMark() {
        super(AbilityType.ABYSS_MARK, "Abyss Mark", 75, 0, SoulType.VOID);
    }

    @Override
    public boolean execute(Player player) {
        LivingEntity target = getTarget(player);

        if (target == null) {
            SoulsSMP.getInstance().getHudManager().showActionBarOverride(player,
                    ChatColor.DARK_PURPLE + "Abyss Mark: " + ChatColor.RED + "No target in range.", ACTION_BAR_OVERRIDE_MILLIS);
            return false;
        }

        if (!SoulsSMP.getInstance().getManaManager().spendMana(player, getManaCost())) {
            SoulsSMP.getInstance().getHudManager().showActionBarOverride(player,
                    ChatColor.DARK_PURPLE + "Abyss Mark: " + ChatColor.RED + "Not enough mana (" + getManaCost() + ").", ACTION_BAR_OVERRIDE_MILLIS);
            return false;
        }

        applyMark(target, player.getUniqueId(), MARK_DURATION_SECONDS);

        player.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.01);
        SoulsSMP.getInstance().getHudManager().showActionBarOverride(player,
                ChatColor.DARK_PURPLE + "Abyss Mark: " + ChatColor.RESET + "Target marked", ACTION_BAR_OVERRIDE_MILLIS);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
        return true;
    }

    private LivingEntity getTarget(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                RANGE,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (result == null || !(result.getHitEntity() instanceof LivingEntity)) return null;
        return (LivingEntity) result.getHitEntity();
    }

    private void applyMark(LivingEntity target, UUID markedBy, int durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        marks.put(target.getUniqueId(), new MarkData(markedBy, expiresAt));
        markedTargetByCaster.put(markedBy, target.getUniqueId());
    }

    public boolean isMarkedBy(LivingEntity target, UUID attacker) {
        MarkData data = marks.get(target.getUniqueId());
        if (data == null) return false;

        if (System.currentTimeMillis() > data.expiresAt) {
            marks.remove(target.getUniqueId());
            markedTargetByCaster.remove(data.markedBy); // Also remove from caster map
            return false;
        }

        return data.markedBy.equals(attacker);
    }

    public int registerHit(LivingEntity target, UUID attacker) {
        MarkData data = marks.get(target.getUniqueId());
        if (data == null || !data.markedBy.equals(attacker)) return -1;

        if (System.currentTimeMillis() > data.expiresAt) {
            marks.remove(target.getUniqueId());
            markedTargetByCaster.remove(data.markedBy); // Also remove from caster map
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
     * Returns the combo hit count on the mark this caster currently has
     * active on their target, or 0 if they have no active mark.
     * Used by the HUD to show combo pips.
     */
    public int getHitCount(UUID casterId) {
        UUID targetId = markedTargetByCaster.get(casterId);
        if (targetId == null) return 0;

        MarkData data = marks.get(targetId);
        if (data == null) return 0;

        return data.hitCount;
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