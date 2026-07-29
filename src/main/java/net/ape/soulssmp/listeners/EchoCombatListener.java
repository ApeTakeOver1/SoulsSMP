package net.ape.soulssmp.listeners;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.abilities.echo.EchoTier;
import net.ape.soulssmp.abilities.echo.passive.EchoPassiveTask;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
 * Handles Echo Soul's combat-driven Noise Meter gains, fake hit sounds,
 * Perfect Echo's attack afterimage/reveal, and Warden immunity.
 *
 * PLACEHOLDER TUNING - all resonance gain amounts below.
 */
public class EchoCombatListener implements Listener {

    private static final double RESONANCE_ON_DEAL_DAMAGE = 8.0;
    private static final double RESONANCE_ON_DEAL_DAMAGE_ECHO_BAND = 12.0; // "gain more resonance from attacking" at Echo tier+
    private static final double RESONANCE_ON_TAKE_DAMAGE = 10.0;
    private static final double RESONANCE_ON_NEARBY_EXPLOSION = 15.0;
    private static final double EXPLOSION_RESONANCE_RADIUS = 15.0;

    private static final long PERFECT_ECHO_REVEAL_MILLIS = 800L; // "position is revealed briefly when attacking"

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
            handleAttackerResonance(attacker, target);
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

    private void handleAttackerResonance(Player attacker, LivingEntity target) {
        PlayerData attackerData = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(attacker);
        if (attackerData.getSoul() != SoulType.ECHO) return;

        EchoTier tierBeforeHit = SoulsSMP.getInstance().getResonanceManager().getTier(attacker);

        double gain = tierBeforeHit == EchoTier.DORMANT ? RESONANCE_ON_DEAL_DAMAGE : RESONANCE_ON_DEAL_DAMAGE_ECHO_BAND;
        SoulsSMP.getInstance().getResonanceManager().addResonance(attacker, gain);

        // Fake hit sound from the Echo band onward - a decoy playing near the attacker.
        if (tierBeforeHit != EchoTier.DORMANT) {
            playFakeHitSound(attacker);
        }

        // Perfect Echo: attacking briefly reveals your real position, with an afterimage burst.
        if (tierBeforeHit == EchoTier.PERFECT_ECHO) {
            EchoPassiveTask.revealBriefly(attacker, PERFECT_ECHO_REVEAL_MILLIS);
            attacker.getWorld().spawnParticle(
                    Particle.REVERSE_PORTAL, attacker.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.02
            );
        }
    }

    private void handleVictimResonance(Player victim) {
        PlayerData victimData = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(victim);
        if (victimData.getSoul() != SoulType.ECHO) return;

        SoulsSMP.getInstance().getResonanceManager().addResonance(victim, RESONANCE_ON_TAKE_DAMAGE);
    }

    private void playFakeHitSound(Player attacker) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location fake = attacker.getLocation().add(
                random.nextDouble(-2, 2), 1, random.nextDouble(-2, 2)
        );
        attacker.getWorld().playSound(fake, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.5f, 1.0f);
    }
}
