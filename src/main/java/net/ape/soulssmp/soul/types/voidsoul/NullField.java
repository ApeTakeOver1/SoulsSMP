package net.ape.soulssmp.soul.types.voidsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class NullField extends Ability {

    private static final double RADIUS = 6.0;
    private static final int DURATION_SECONDS = 8;

    public NullField() {
        super(AbilityType.NULL_FIELD, "Null Field", 50, 60);
    }

    @Override
    public boolean execute(Player player) {
        if (!SoulsSMP.getInstance().getManaManager().spendMana(player, getManaCost())) {
            player.sendMessage("§8§lVoid §7» §cNot enough mana for Null Field.");
            return false;
        }

        Location center = player.getLocation();

        SoulsSMP.getInstance().getNullFieldManager()
                .createField(player, center, RADIUS, DURATION_SECONDS);

        playActivationVisual(player, center);
        player.sendMessage("§8§lVoid §7» §5Null Field opened. Abilities weaken within it.");
        return true;
    }

    private void playActivationVisual(Player player, Location center) {
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.8f);
        player.getWorld().playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 0.6f);

        // Full hollow sphere made of white crack particles - the "domain" shape
        int latitudeSteps = 18;
        int longitudeSteps = 24;

        for (int lat = 0; lat <= latitudeSteps; lat++) {
            double theta = Math.PI * lat / latitudeSteps; // 0 to PI
            double y = RADIUS * Math.cos(theta);
            double ringRadius = RADIUS * Math.sin(theta);

            for (int lon = 0; lon < longitudeSteps; lon++) {
                double phi = 2 * Math.PI * lon / longitudeSteps;
                double x = ringRadius * Math.cos(phi);
                double z = ringRadius * Math.sin(phi);

                Location point = center.clone().add(x, y, z);
                player.getWorld().spawnParticle(
                        Particle.DUST, point, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(230, 230, 255), 1.2f)
                );
            }
        }

        // Ground crack ring for extra emphasis at the base
        for (int i = 0; i < 360; i += 6) {
            double rad = Math.toRadians(i);
            double x = center.getX() + RADIUS * Math.cos(rad);
            double z = center.getZ() + RADIUS * Math.sin(rad);
            Location edge = new Location(center.getWorld(), x, center.getY(), z);
            player.getWorld().spawnParticle(Particle.END_ROD, edge, 2, 0, 0.2, 0, 0.01);
        }
    }
}