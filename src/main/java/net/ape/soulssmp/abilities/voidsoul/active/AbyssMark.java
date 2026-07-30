package net.ape.soulssmp.abilities.voidsoul.active;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.Ability;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.api.SoulType;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reworked from scratch - the old version's HUD ("Marked (Xs)" + combo
 * pips) never displayed reliably and the 3rd/5th-hit combo bonuses,
 * mana refund, and Null-Field-conditional stun added a lot of surface
 * area for that. Abyss Mark now works exactly like Hemorrhage: fixed
 * mana cost, marks a target, builds a stack 1-5 on hits, and simply
 * stops once it reaches 5 - no rupture-style burst, no refund, no stun.
 * VoidCombatListener just increments the stack now; HudTask reads it
 * the same way it reads Hemorrhage's stack status.
 */
public class AbyssMark extends Ability {

    private static final double RANGE = 30.0;
    private static final int MARK_DURATION_SECONDS = 30;
    private static final long ACTION_BAR_OVERRIDE_MILLIS = 1500L;

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

    public AbyssMark() {
        super(AbilityType.ABYSS_MARK, "Abyss Mark", 25, 20, SoulType.VOID);
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

        applyMark(target.getUniqueId(), player.getUniqueId(), MARK_DURATION_SECONDS);

        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.01);
        SoulsSMP.getInstance().getHudManager().showActionBarOverride(player,
                ChatColor.DARK_PURPLE + "Abyss Mark: " + ChatColor.RESET + "Target marked", ACTION_BAR_OVERRIDE_MILLIS);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
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

    private void applyMark(UUID targetId, UUID casterId, int durationSeconds) {
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
     * target, or null if they have no active mark. Same shape as
     * Hemorrhage.getCurrentMarkStatus() so HudTask can read it the same way.
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