package net.ape.soulssmp.listeners;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.abilities.echo.passive.EchoAfterimage;
import net.ape.soulssmp.abilities.echo.EchoTier;
import net.ape.soulssmp.abilities.echo.passive.EchoPassiveTask;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.api.soul.SoulDefinition;
import net.ape.soulssmp.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles Echo Soul's combat-driven Noise Meter gains, Perfect Echo's
 * bonus damage + random attack afterimage, the brief reveal-after-hit for
 * the transparent tiers, and Warden immunity.
 *
 * PLACEHOLDER TUNING - all resonance gain amounts, the Perfect Echo damage
 * multiplier, and the afterimage chance below.
 */
public class EchoCombatListener implements Listener {

    private static final double RESONANCE_ON_DEAL_DAMAGE = 8.0;
    private static final double RESONANCE_ON_DEAL_DAMAGE_ECHO_BAND = 12.0; // "gain more resonance from attacking" at Echo tier+
    private static final double RESONANCE_ON_TAKE_DAMAGE = 10.0;
    private static final double RESONANCE_ON_NEARBY_EXPLOSION = 15.0;
    private static final double EXPLOSION_RESONANCE_RADIUS = 15.0;

    private static final double PERFECT_ECHO_DAMAGE_MULTIPLIER = 1.3; // "more damage" at Perfect Echo
    private static final double PERFECT_ECHO_AFTERIMAGE_CHANCE = 0.5; // afterimage is random, not on every hit

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Warden immunity: no Sonic Boom damage against an Echo soul.
        if (event.getDamager() instanceof Warden && event.getCause() == EntityDamageEvent.DamageCause.SONIC_BOOM
                && target instanceof Player targetPlayer) {
            PlayerData targetData = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(targetPlayer);
            if (targetData.getSoul() == SoulType.ECHO) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getDamager() instanceof Player attacker) {
            handleAttackerResonance(attacker, target, event);
        }

        if (target instanceof Player victim) {
            handleVictimResonance(victim);
        }
    }

    @EventHandler
    public void onWardenTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Warden)) return;
        if (!(event.getTarget() instanceof Player targetPlayer)) return;

        PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(targetPlayer);
        if (data.getSoul() == SoulType.ECHO) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        Location center = event.getLocation();
        if (center.getWorld() == null) return;

        for (Player player : center.getWorld().getPlayers()) {
            PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);
            if (data.getSoul() != SoulType.ECHO) continue;

            if (player.getLocation().distance(center) <= EXPLOSION_RESONANCE_RADIUS) {
                SoulsSMP.getInstance().getResonanceManager().addResonance(player, RESONANCE_ON_NEARBY_EXPLOSION);
            }
        }
    }

    private void handleAttackerResonance(Player attacker, LivingEntity target, EntityDamageByEntityEvent event) {
        PlayerData attackerData = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(attacker);
        if (attackerData.getSoul() != SoulType.ECHO) return;

        EchoTier tierBeforeHit = SoulsSMP.getInstance().getResonanceManager().getTier(attacker);

        double gain = tierBeforeHit == EchoTier.DORMANT ? RESONANCE_ON_DEAL_DAMAGE : RESONANCE_ON_DEAL_DAMAGE_ECHO_BAND;
        SoulsSMP.getInstance().getResonanceManager().addResonance(attacker, gain);

        if (tierBeforeHit == EchoTier.DISTORTED_ECHO || tierBeforeHit == EchoTier.LOST_SIGNAL) {
            // Landing a hit briefly gives your position away, then you fade back out.
            EchoPassiveTask passiveTask = findEchoPassiveTask();
            if (passiveTask != null) passiveTask.markRevealedAfterHit(attacker.getUniqueId());
        }

        // Perfect Echo: bonus damage always applies; the afterimage is a random chance, not guaranteed.
        if (tierBeforeHit == EchoTier.PERFECT_ECHO) {
            event.setDamage(event.getDamage() * PERFECT_ECHO_DAMAGE_MULTIPLIER);

            if (ThreadLocalRandom.current().nextDouble() < PERFECT_ECHO_AFTERIMAGE_CHANCE) {
                EchoAfterimage.spawn(attacker, target);
            }
        }
    }

    private void handleVictimResonance(Player victim) {
        PlayerData victimData = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(victim);
        if (victimData.getSoul() != SoulType.ECHO) return;

        SoulsSMP.getInstance().getResonanceManager().addResonance(victim, RESONANCE_ON_TAKE_DAMAGE);
    }

    private EchoPassiveTask findEchoPassiveTask() {
        SoulDefinition definition = SoulsSMP.getInstance().getSoulRegistry().get(SoulType.ECHO);
        if (definition == null) return null;

        for (SoulDefinition.PassiveSchedule passive : definition.getPassives()) {
            if (passive.task() instanceof EchoPassiveTask echoPassiveTask) {
                return echoPassiveTask;
            }
        }
        return null;
    }
}