package net.ape.soulssmp.soul.types.bloodsoul;

import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.Particle;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class Hemorrhage extends Ability {

    private static final double RANGE = 20.0;
    private static final int MARK_DURATION_SECONDS = 30;

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

        SoulsSMP.getInstance().getHemorrhageManager()
                .applyMark(target.getUniqueId(), player.getUniqueId(), MARK_DURATION_SECONDS);

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
}