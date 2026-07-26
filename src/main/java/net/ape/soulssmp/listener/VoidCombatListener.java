package net.ape.soulssmp.listener;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.soul.types.voidsoul.NullFieldManager;
import net.ape.soulssmp.soul.types.voidsoul.VoidMarkManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class VoidCombatListener implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        VoidMarkManager markManager = SoulsSMP.getInstance().getVoidMarkManager();
        NullFieldManager fieldManager = SoulsSMP.getInstance().getNullFieldManager();

        handleAbyssMarkCombo(event, attacker, target, markManager);
        handleNullFieldStun(attacker, target, fieldManager, markManager);
    }

    private void handleAbyssMarkCombo(EntityDamageByEntityEvent event, Player attacker,
                                      LivingEntity target, VoidMarkManager markManager) {

        if (!markManager.isMarkedBy(target, attacker.getUniqueId())) return;

        int hitCount = markManager.registerHit(target, attacker.getUniqueId());
        if (hitCount <= 0) return;

        if (hitCount % 3 == 0) {
            event.setDamage(event.getDamage() * 1.25);
            target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.3f);
        }

        if (hitCount % 5 == 0) {
            int manaGain = 10;
            SoulsSMP.getInstance().getManaManager().addMana(attacker, manaGain);
            attacker.sendMessage("§8§lVoid §7» §5Combo surge! §f+" + manaGain + " mana.");
            attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.2f);
        }
    }

    private void handleNullFieldStun(Player attacker, LivingEntity target,
                                     NullFieldManager fieldManager, VoidMarkManager markManager) {

        UUID fieldOwner = fieldManager.getFieldOwnerAt(target.getLocation());
        if (fieldOwner == null || !fieldOwner.equals(attacker.getUniqueId())) return;

        int fieldHits = fieldManager.registerFieldHit(target);

        if (fieldHits >= 5) {
            fieldManager.clearFieldHits(target);
            markManager.applyMark(target, attacker.getUniqueId(), 10);
            applyStun(attacker, target);
            attacker.sendMessage("§8§lVoid §7» §5Target stunned by the void.");
        }
    }

    /**
     * Vanilla has no true "stun" status, so this approximates one with a short,
     * heavy Slowness + Weakness combo, plus a particle "chain" linking attacker
     * to target and chain sound effects for the visual/audio feel of being bound.
     */
    private void applyStun(Player attacker, LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 8));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 2));

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.7f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 0.8f, 0.9f);
        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.01);

        drawChainEffect(attacker.getEyeLocation(), target.getLocation().add(0, 1, 0));
    }

    private void drawChainEffect(Location from, Location to) {
        Vector direction = to.clone().subtract(from).toVector();
        double distance = direction.length();
        direction.normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Location point = from.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(
                    Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(40, 40, 40), 1.0f)
            );
        }
    }
}