package net.ape.soulssmp.soul.types.bloodsoul;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
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

public class Curse extends Ability {

    private static final int STUN_TICKS = 60;
    private static final double RANGE = 20.0;
    private static final double DARKNESS_RADIUS = 10.0;
    private static final int RESTRICTION_DURATION_SECONDS = 20;
    private static final int SILENCE_DURATION_SECONDS = 30;
    private static final int PUPPET_DURATION_SECONDS = 20;
    private static final double PUPPET_RADIUS = 5.0;

    private static final double CLOUD_HEIGHT = 6.0;
    private static final double CLOUD_RADIUS = 2.0;
    private static final int RAIN_DURATION_TICKS = 60;

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
                    new Particle.DustOptions(Color.fromRGB(120, 0, 0), 1.4f)
            );
        }
    }

    private void resolveCurse(Player player) {
        LivingEntity target = getLockedTarget(player);

        if (target == null) {
            player.sendMessage("§4§lBlood §7» §7The curse found no target and fades.");
            return;
        }

        rainBloodCloud(target);

        SoulsSMP.getInstance().getCursePendingManager().setPendingTarget(player.getUniqueId(), target);
        CurseGUI.open(player);
    }

    public void rollWeightedEffect(LivingEntity target, Player caster) {
        double roll = random.nextDouble();

        if (roll < 0.25) {
            applyPuppet(target, caster);
        } else if (roll < 0.625) {
            applyRestriction(target, caster);
        } else {
            applySilence(target, caster);
        }
    }

    private LivingEntity getLockedTarget(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                RANGE,
                0.4,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (result == null || !(result.getHitEntity() instanceof LivingEntity)) return null;
        return (LivingEntity) result.getHitEntity();
    }

    private void rainBloodCloud(LivingEntity target) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= RAIN_DURATION_TICKS || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                Location base = target.getLocation();
                Location cloudCenter = base.clone().add(0, CLOUD_HEIGHT, 0);

                drawCloudDisc(cloudCenter);
                drawRaindrops(base, cloudCenter);

                if (elapsed % 6 == 0) {
                    base.getWorld().playSound(base, Sound.WEATHER_RAIN_ABOVE, 0.6f, 0.4f);
                }

                elapsed += 2;
            }
        }.runTaskTimer(SoulsSMP.getInstance(), 0L, 2L);
    }

    private void drawCloudDisc(Location cloudCenter) {
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * CLOUD_RADIUS;
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;

            Location point = cloudCenter.clone().add(x, (random.nextDouble() - 0.5) * 0.6, z);
            cloudCenter.getWorld().spawnParticle(
                    Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(80, 0, 0), 2.2f)
            );
        }
    }

    private void drawRaindrops(Location base, Location cloudCenter) {
        for (int i = 0; i < 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * CLOUD_RADIUS * 1.6;
            double offsetZ = (random.nextDouble() - 0.5) * CLOUD_RADIUS * 1.6;
            double dropHeight = random.nextDouble() * CLOUD_HEIGHT;

            Location dropPoint = base.clone().add(offsetX, dropHeight, offsetZ);
            base.getWorld().spawnParticle(
                    Particle.DUST, dropPoint, 1, 0.02, 0.05, 0.02, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.1f)
            );
        }
    }

    /**
     * No more JUMP_BOOST hack - actual jump prevention now comes from
     * RestrictionJumpListener cancelling PlayerJumpEvent directly.
     * Slowness still applies here for the movement-speed part.
     */
    public void applyRestriction(LivingEntity target, Player caster) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, RESTRICTION_DURATION_SECONDS * 20, 2, false, true, true));

        if (target instanceof Player targetPlayer) {
            SoulsSMP.getInstance().getRestrictionManager().applyRestriction(targetPlayer, RESTRICTION_DURATION_SECONDS);
            targetPlayer.sendTitle("§4§lRESTRICTED", "§7No sprint, slowed, can't jump", 10, 60, 10);
            targetPlayer.sendMessage("§4§lBlood §7» §cYou've been Restricted by the Curse.");
        }

        caster.sendMessage("§4§lBlood §7» §cCurse landed: §fRestriction");
    }

    public void applySilence(LivingEntity target, Player caster) {
        if (target instanceof Player targetPlayer) {
            SoulsSMP.getInstance().getSilenceManager().applySilence(targetPlayer, SILENCE_DURATION_SECONDS);
        }

        caster.sendMessage("§4§lBlood §7» §cCurse landed: §fSilence");
    }

    public void applyPuppet(LivingEntity target, Player caster) {
        if (!(target instanceof Player targetPlayer)) return;

        SoulsSMP.getInstance().getRestraintManager()
                .addRestraint(targetPlayer.getUniqueId(), caster.getUniqueId(), PUPPET_RADIUS, false, PUPPET_DURATION_SECONDS);

        randomizeHotbar(targetPlayer);

        targetPlayer.sendTitle("§4§lPUPPET", "§7You are bound to the Blood Soul", 10, 60, 10);
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