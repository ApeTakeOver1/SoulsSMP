package net.ape.soulssmp.abilities.voidsoul.ultimate;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.Ability;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.api.SoulType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NullField extends Ability {

    private static final double RADIUS = 6.0;
    private static final int WEAKEN_DURATION_SECONDS = 45;
    private static final int WEAKEN_DURATION_TICKS = WEAKEN_DURATION_SECONDS * 20;

    // "Null Field affected" + mana boost tracking (formerly NullFieldManager) -
    // NullField owns this state; VoidCombatListener and ManaTask read it via
    // SoulsSMP.getInstance().getAbilityManager().getAs(AbilityType.NULL_FIELD, NullField.class).
    private static class AffectedData {
        long expiresAt;

        AffectedData(long expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    // casterId -> (targetId -> data)
    private final Map<UUID, Map<UUID, AffectedData>> affected = new HashMap<>();

    // casterId -> mana gain boost expiry (active while Null Field's effects are live)
    private final Map<UUID, Long> manaBoostExpiry = new HashMap<>();

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
        activateManaBoost(player.getUniqueId(), WEAKEN_DURATION_SECONDS);
        player.sendMessage("§8§lVoid §7» §5Null Field flashed. Caught enemies are weakened for " + WEAKEN_DURATION_SECONDS + "s.");
        player.sendMessage("§8§lVoid §7» §5Your mana gain is boosted while Null Field is active.");
        return true;
    }

    private void catchNearbyPlayers(Player caster, Location center) {
        for (var entity : center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.getUniqueId().equals(caster.getUniqueId())) continue;

            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, WEAKEN_DURATION_TICKS, 0, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, WEAKEN_DURATION_TICKS, 0, false, true, true));

            markAffected(caster.getUniqueId(), target.getUniqueId(), WEAKEN_DURATION_SECONDS);

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

        // FLASH's data type is (incorrectly) declared as org.bukkit.Color in this
        // Paper 1.21.11 API build, so the no-data overload throws IllegalArgumentException
        // at runtime ("missing required data class org.bukkit.Color"). Supply a color to
        // satisfy the check; it has no visible effect on the particle itself.
        player.getWorld().spawnParticle(Particle.FLASH, center, 1, org.bukkit.Color.WHITE);
        player.getWorld().spawnParticle(Particle.END_ROD, center, 40, RADIUS / 2, 1, RADIUS / 2, 0.05);
    }

    private void markAffected(UUID casterId, UUID targetId, int durationSeconds) {
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

    private void activateManaBoost(UUID casterId, int durationSeconds) {
        manaBoostExpiry.put(casterId, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    public boolean isManaBoostActive(UUID casterId) {
        Long expiresAt = manaBoostExpiry.get(casterId);
        if (expiresAt == null) return false;

        if (System.currentTimeMillis() > expiresAt) {
            manaBoostExpiry.remove(casterId);
            return false;
        }

        return true;
    }
}