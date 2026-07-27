package net.ape.soulssmp.soul.types.voidsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NullField extends Ability {

    private static final double RADIUS = 6.0;
    private static final int WEAKEN_DURATION_SECONDS = 45;
    private static final int WEAKEN_DURATION_TICKS = WEAKEN_DURATION_SECONDS * 20;

    public NullField() {
        super(AbilityType.NULL_FIELD, "Null Field", 50, 90, SoulType.VOID);
    }

    @Override
    public boolean execute(Player player) {
        if (!SoulsSMP.getInstance().getManaManager().spendMana(player, getManaCost())) {
            player.sendMessage("§8§lVoid §7» §cNot enough mana for Null Field.");
            return false;
        }

        Location center = player.getLocation();

        catchNearbyPlayers(player, center);
        playInstantFlash(player, center);
        player.sendMessage("§8§lVoid §7» §5Null Field flashed. Caught enemies are weakened for " + WEAKEN_DURATION_SECONDS + "s.");
        return true;
    }

    private void catchNearbyPlayers(Player caster, Location center) {
        for (var entity : center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.getUniqueId().equals(caster.getUniqueId())) continue;

            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, WEAKEN_DURATION_TICKS, 0, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, WEAKEN_DURATION_TICKS, 0, false, true, true));

            SoulsSMP.getInstance().getNullFieldManager()
                    .markAffected(caster.getUniqueId(), target.getUniqueId(), WEAKEN_DURATION_SECONDS);

            target.sendMessage("§8§lVoid §7» §5You've been caught in the Null Field.");
        }
    }

    private void playInstantFlash(Player player, Location center) {
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.8f);
        player.getWorld().playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 0.6f);
        player.getWorld().playSound(center, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.5f, 1.5f);

        int latitudeSteps = 18;
        int longitudeSteps = 24;

        for (int lat = 0; lat <= latitudeSteps; lat++) {
            double theta = Math.PI * lat / latitudeSteps;
            double y = RADIUS * Math.cos(theta);
            double ringRadius = RADIUS * Math.sin(theta);

            for (int lon = 0; lon < longitudeSteps; lon++) {
                double phi = 2 * Math.PI * lon / longitudeSteps;
                double x = ringRadius * Math.cos(phi);
                double z = ringRadius * Math.sin(phi);

                Location point = center.clone().add(x, y, z);
                player.getWorld().spawnParticle(
                        Particle.DUST, point, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 255), 1.5f)
                );
            }
        }

        player.getWorld().spawnParticle(Particle.FLASH, center, 1);
        player.getWorld().spawnParticle(Particle.END_ROD, center, 40, RADIUS / 2, 1, RADIUS / 2, 0.05);
    }
}