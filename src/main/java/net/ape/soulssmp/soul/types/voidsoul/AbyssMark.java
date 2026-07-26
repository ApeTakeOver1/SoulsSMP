package net.ape.soulssmp.soul.types.voidsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class AbyssMark extends Ability {

    private static final double RANGE = 8.0;
    private static final int MARK_DURATION_SECONDS = 10;

    public AbyssMark() {
        super(AbilityType.ABYSS_MARK, "Abyss Mark", 15, 12);
    }

    @Override
    public boolean execute(Player player) {
        LivingEntity target = getTarget(player);

        if (target == null) {
            player.sendMessage("§8§lVoid §7» §cNo target in range.");
            return false;
        }

        if (!SoulsSMP.getInstance().getManaManager().spendMana(player, getManaCost())) {
            player.sendMessage("§8§lVoid §7» §cNot enough mana.");
            return false;
        }

        SoulsSMP.getInstance().getVoidMarkManager()
                .applyMark(target, player.getUniqueId(), MARK_DURATION_SECONDS);

        player.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.01);
        player.sendMessage("§8§lVoid §7» §5Target marked.");
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