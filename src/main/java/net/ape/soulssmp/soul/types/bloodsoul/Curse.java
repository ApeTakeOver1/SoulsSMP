package net.ape.soulssmp.soul.types.bloodsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FallingBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Stuns the caster for 3s (Slowness/no-jump + ritual visuals + Darkness on
 * nearby players), then locks onto whatever the caster is looking at when
 * the channel ends, and applies one of 3 random debuffs to that target.
 */
public class Curse extends Ability {

    private static final int STUN_TICKS = 60; // 3s
    private static final double RANGE = 20.0;
    private static final double DARKNESS_RADIUS = 10.0;
    private static final int RESTRICTION_DURATION_SECONDS = 20;
    private static final int SILENCE_DURATION_SECONDS = 30;
    private static final int PUPPET_DURATION_SECONDS = 20;
    private static final double PUPPET_RADIUS = 5.0;

    private final Random random = new Random();

    public Curse() {
        super(AbilityType.CURSE, "Curse", 40, 30, SoulType.BLOOD);
    }

    @Override
    public boolean execute(Player player) {
        if (!SoulsSMP.getInstance().getManaManager().spendMana(player, getManaCost())) {
            player.sendMessage("§4§lBlood §7» §cNot enough mana for Curse.");
            return false;
        }

        channelStun(player);
        playRitualVisual(player);
        player.sendMessage("§4§lBlood §7» §cThe ritual begins...");

        Bukkit.getScheduler().runTaskLater(SoulsSMP.getInstance(), () -> resolveCurse(player), STUN_TICKS);
        return true;
    }

    private void channelStun(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, STUN_TICKS, 250, false, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, STUN_TICKS, -10, false, false, false));
    }

    private void playRitualVisual(Player player) {
        Location center = player.getLocation();

        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_ROAR, 0.5f, 0.6f);
        player.getWorld().playSound(center, Sound.ITEM_TOTEM_USE, 0.4f, 0.5f);

        for (var entity : center.getWorld().getNearbyEntities(center, DARKNESS_RADIUS, DARKNESS_RADIUS, DARKNESS_RADIUS)) {
            if (entity instanceof Player nearby) {
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, STUN_TICKS + 20, 0, false, false, false));
            }
        }

        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double x = center.getX() + 2.5 * Math.cos(rad);
            double z = center.getZ() + 2.5 * Math.sin(rad);
            Location edge = new Location(center.getWorld(), x, center.getY() + 0.1, z);
            center.getWorld().spawnParticle(
                    Particle.DUST, edge, 2, 0, 0.1, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(140, 0, 0), 1.4f)
            );
        }
    }

    private void resolveCurse(Player player) {
        LivingEntity target = getLockedTarget(player);

        if (target == null) {
            player.sendMessage("§4§lBlood §7» §7The curse found no target and fades.");
            return;
        }

        rainBloodCloud(target.getLocation());

        int roll = random.nextInt(3);
        switch (roll) {
            case 0 -> applyRestriction(target, player);
            case 1 -> applySilence(target, player);
            case 2 -> applyPuppet(target, player);
        }
    }

    private LivingEntity getLockedTarget(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                RANGE,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (result == null || !(result.getHitEntity() instanceof LivingEntity)) return null;
        return (LivingEntity) result.getHitEntity();
    }

    private void rainBloodCloud(Location target) {
        Location cloudCenter = target.clone().add(0, 6, 0);

        for (int i = 0; i < 6; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 2;
            double offsetZ = (random.nextDouble() - 0.5) * 2;
            Location spawnAt = cloudCenter.clone().add(offsetX, 0, offsetZ);

            FallingBlock fallingBlock = spawnAt.getWorld().spawnFallingBlock(
                    spawnAt, Material.RED_STAINED_GLASS.createBlockData()
            );
            fallingBlock.setDropItem(false);
            fallingBlock.setHurtEntities(false);
        }

        target.getWorld().spawnParticle(Particle.DRIPPING_DRAGON_BREATH, cloudCenter, 15, 0.5, 0.3, 0.5, 0.02);
        target.getWorld().playSound(target, Sound.WEATHER_RAIN_ABOVE, 0.6f, 0.5f);
    }

    private void applyRestriction(LivingEntity target, Player caster) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, RESTRICTION_DURATION_SECONDS * 20, 1, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, RESTRICTION_DURATION_SECONDS * 20, -1, false, true, true));

        if (target instanceof Player targetPlayer) {
            SoulsSMP.getInstance().getRestrictionManager().applyRestriction(targetPlayer, RESTRICTION_DURATION_SECONDS);
            targetPlayer.sendMessage("§4§lBlood §7» §cYou've been Restricted by the Curse.");
        }

        caster.sendMessage("§4§lBlood §7» §cCurse landed: §fRestriction");
    }

    private void applySilence(LivingEntity target, Player caster) {
        if (target instanceof Player targetPlayer) {
            SoulsSMP.getInstance().getSilenceManager().applySilence(targetPlayer, SILENCE_DURATION_SECONDS);
            targetPlayer.sendMessage("§4§lBlood §7» §cYou've been Silenced by the Curse.");
        }

        caster.sendMessage("§4§lBlood §7» §cCurse landed: §fSilence");
    }

    private void applyPuppet(LivingEntity target, Player caster) {
        if (!(target instanceof Player targetPlayer)) return;

        // Immediate drag-in on application
        Location pullTo = caster.getLocation().clone().add(caster.getLocation().getDirection().multiply(-1.5));
        targetPlayer.teleport(pullTo);

        SoulsSMP.getInstance().getRestraintManager()
                .addRestraint(targetPlayer.getUniqueId(), caster.getUniqueId(), PUPPET_RADIUS, true, PUPPET_DURATION_SECONDS);

        randomizeHotbar(targetPlayer);

        targetPlayer.sendMessage("§4§lBlood §7» §cYou are now a Puppet of the Blood Soul.");
        caster.sendMessage("§4§lBlood §7» §cCurse landed: §fPuppet");
    }

    private void randomizeHotbar(Player player) {
        List<ItemStack> hotbarItems = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            hotbarItems.add(player.getInventory().getItem(i));
        }

        Collections.shuffle(hotbarItems, random);

        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, hotbarItems.get(i));
        }
    }
}