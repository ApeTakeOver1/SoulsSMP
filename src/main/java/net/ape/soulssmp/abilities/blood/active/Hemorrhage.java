package net.ape.soulssmp.abilities.blood.active;

import net.ape.soulssmp.api.Ability;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.SoulType;
import org.bukkit.Particle;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Hemorrhage extends Ability {

    private static final double RANGE = 20.0;
    private static final int MARK_DURATION_SECONDS = 30;

    // Mark/stack tracking (formerly HemorrhageManager) - Hemorrhage owns this
    // state; BloodCombatListener and HudTask read it via
    // SoulsSMP.getInstance().getAbilityManager().getAs(AbilityType.HEMORRHAGE, Hemorrhage.class).
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

    public Hemorrhage() {
        super(AbilityType.HEMORRHAGE, "Hemorrhage", 25, 20, SoulType.BLOOD);
    }

    @Override
    public boolean execute(Player player) {
        LivingEntity target = getTarget(player);

        if (target == null) {
            player.sendTitle("Hemorrhage", ChatColor.RED + "No target in range.", 0, 40, 10);
            return false;
        }

        if (!SoulsSMP.getInstance().getManaManager().spendMana(player, getManaCost())) {
            player.sendTitle("Hemorrhage", ChatColor.RED + "Not enough mana.", 0, 40, 10);
            return false;
        }

        applyMark(target.getUniqueId(), player.getUniqueId(), MARK_DURATION_SECONDS);

        target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 1, 0), 20,
                0.3, 0.5, 0.3, org.bukkit.Material.REDSTONE_BLOCK.createBlockData());
        player.sendTitle("Hemorrhage", ChatColor.RED + "Target Marked", 0, 40, 10);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.0f);
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