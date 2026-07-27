package net.ape.soulssmp.soul.types.voidsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class AbyssMark extends Ability {

    private static final double RANGE = 30.0;
    private static final int MARK_DURATION_SECONDS = 10;

    public AbyssMark() {
        super(AbilityType.ABYSS_MARK, "Abyss Mark", 0, 0, SoulType.VOID);
    }

    @Override
    public boolean execute(Player player) {
        LivingEntity target = getTarget(player);

        if (target == null) {
            player.sendMessage("§8§lVoid §7» §cNo target in range.");
            return false;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            int currentMana = SoulsSMP.getInstance().getManaManager().getMana(player);
            if (currentMana <= 0) {
                player.sendMessage("§8§lVoid §7» §cYou have no mana to mark with.");
                return false;
            }
            SoulsSMP.getInstance().getManaManager().setMana(player, 0);
        }

        SoulsSMP.getInstance().getVoidMarkManager()
                .applyMark(target, player.getUniqueId(), MARK_DURATION_SECONDS);

        player.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.01);
        player.sendMessage("§8§lVoid §7» §5Target marked. §7(All mana spent)");
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