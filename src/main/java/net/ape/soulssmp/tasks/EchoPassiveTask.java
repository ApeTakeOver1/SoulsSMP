package net.ape.soulssmp.abilities.echo.passive;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.abilities.echo.EchoPacketService;
import net.ape.soulssmp.abilities.echo.EchoTier;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles Echo Soul's passive: baseline Speed, the Noise Meter's tier-based
 * distortion effects (transparency, quiet footsteps, hidden nametag), Lost
 * Signal's full invisibility + periodic location pulse, and Perfect Echo's
 * full invisibility + attack-speed/damage buff. Perfect Echo's afterimage
 * visual itself is triggered from EchoCombatListener (see EchoAfterimage)
 * since it only fires on a hit.
 *
 * Runs every 5 ticks (0.25s) so flicker/pulse/nametag/silence state stays
 * responsive.
 *
 * Tier bands (per current design):
 *   0%          - Speed II, nothing else.
 *   0-25%       - Echo: Speed III, extra Resonance on hit (see EchoCombatListener).
 *   25-50%      - Distorted Echo: slight transparency (brief flicker), quiet
 *                 footsteps, hidden nametag.
 *   50-75%      - Lost Signal: FULL invisibility (armor hidden too), but
 *                 every ~5 seconds your location is briefly revealed as a
 *                 "signal pulse" (forced visible for ~1s). Real footsteps
 *                 stay quiet, and illusion footstep particles appear too.
 *   75-100%     - Perfect Echo: FULL invisibility (armor hidden too),
 *                 permanently - plus faster attack speed and more damage.
 *                 Attacking someone reveals your location via an afterimage
 *                 (handled by EchoCombatListener/EchoAfterimage) and forces
 *                 you visible for a short window, same as the old hit-reveal.
 *
 * PLACEHOLDER TUNING - Distorted Echo's flicker duty-cycle, the Lost Signal
 * pulse interval/duration, ambient illusion chance, and the Perfect Echo
 * attack speed bonus are all easy to retune once tested in-game.
 */
public class EchoPassiveTask extends BukkitRunnable {

    private static final String HIDDEN_TAG_TEAM_NAME = "soulssmp_echo_hidden";

    private static final int CYCLE_RUNS = 8; // 8 runs * 5 ticks = 2 seconds per flicker cycle (Distorted Echo only)
    private static final int INVISIBILITY_EFFECT_TICKS = 7; // slightly longer than the 5-tick run gap

    // How much of the flicker cycle is spent transparent. 0 = fully visible. Distorted Echo only now.
    private static final double DISTORTED_ECHO_TRANSPARENT_FRACTION = 0.15; // "slight transparency"

    private static final long HIT_REVEAL_MILLIS = 700; // brief forced-visible window after landing a hit (Perfect Echo)

    private static final long LOST_SIGNAL_PULSE_INTERVAL_MILLIS = 5000L; // "every 5 seconds or so"
    private static final long LOST_SIGNAL_PULSE_REVEAL_MILLIS = 1000L; // how long the location-reveal pulse lasts

    private static final double ILLUSION_FOOTSTEP_CHANCE_PER_RUN = 0.20; // Lost Signal only

    private static final NamespacedKey PERFECT_ECHO_ATTACK_SPEED_KEY =
            new NamespacedKey("soulssmp", "perfect_echo_attack_speed");
    private static final double PERFECT_ECHO_ATTACK_SPEED_BONUS = 0.35; // +35% attack speed

    private final Set<UUID> nametagHidden = new HashSet<>();
    private final Set<UUID> footstepsSilenced = new HashSet<>();
    private final Set<UUID> attackSpeedBuffed = new HashSet<>();

    // Forced-fully-visible window: used both for the post-hit reveal (Perfect Echo)
    // and the periodic location pulse (Lost Signal).
    private final Map<UUID, Long> revealedUntilMillis = new HashMap<>();
    private final Map<UUID, Long> lastLostSignalPulseAt = new HashMap<>();

    /**
     * Called by EchoCombatListener right after a hit lands while the
     * attacker is in Perfect Echo - forces them fully visible for a short
     * window (the afterimage moment) instead of staying hidden.
     */
    public void markRevealedAfterHit(UUID playerId) {
        revealedUntilMillis.put(playerId, System.currentTimeMillis() + HIT_REVEAL_MILLIS);
    }

    private int tickCounter = 0;

    @Override
    public void run() {
        tickCounter++;

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);

                if (data.getSoul() != SoulType.ECHO) {
                    cleanupIfNeeded(player);
                    continue;
                }

                EchoTier tier = SoulsSMP.getInstance().getResonanceManager().getTier(player);

                applySpeed(player, tier);
                applyTransparency(player, tier);
                applyNametagVisibility(player, tier);
                applyFootstepSilence(player, tier);
                applyAmbientIllusions(player, tier);
                applyPerfectEchoBuffs(player, tier);
            } catch (Exception e) {
                SoulsSMP.getInstance().getLogger().warning(
                        "EchoPassiveTask error for " + player.getName() + ": " + e.getMessage()
                );
            }
        }
    }

    private void applySpeed(Player player, EchoTier tier) {
        int amplifier = (tier == EchoTier.DORMANT) ? 1 : 2; // Speed II baseline, Speed III from Echo tier onward
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, amplifier, false, true, true));
    }

    private void applyTransparency(Player player, EchoTier tier) {
        UUID id = player.getUniqueId();

        // A forced-visible window (post-hit reveal, or Lost Signal's periodic pulse)
        // always wins over whatever the tier would normally do.
        Long revealUntil = revealedUntilMillis.get(id);
        if (revealUntil != null) {
            if (System.currentTimeMillis() < revealUntil) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                EchoPacketService.revealEquipment(player);
                return;
            }
            revealedUntilMillis.remove(id);
        }

        switch (tier) {
            case DISTORTED_ECHO -> applyFlicker(player);
            case LOST_SIGNAL -> applyLostSignal(player);
            case PERFECT_ECHO -> applyFullInvisibility(player);
            default -> {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                EchoPacketService.revealEquipment(player);
            }
        }
    }

    /** Distorted Echo only: brief, mostly-visible flicker to simulate "slight transparency". */
    private void applyFlicker(Player player) {
        int transparentRuns = (int) Math.round(CYCLE_RUNS * DISTORTED_ECHO_TRANSPARENT_FRACTION);
        boolean shouldBeTransparentThisRun = (tickCounter % CYCLE_RUNS) < transparentRuns;

        if (shouldBeTransparentThisRun) {
            applyFullInvisibility(player);
        } else {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            EchoPacketService.revealEquipment(player);
        }
    }

    /** Lost Signal: stays fully invisible, but pulses fully visible for ~1s every ~5s. */
    private void applyLostSignal(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastPulse = lastLostSignalPulseAt.get(id);

        if (lastPulse == null || now - lastPulse >= LOST_SIGNAL_PULSE_INTERVAL_MILLIS) {
            lastLostSignalPulseAt.put(id, now);
            revealedUntilMillis.put(id, now + LOST_SIGNAL_PULSE_REVEAL_MILLIS);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            EchoPacketService.revealEquipment(player);
            return;
        }

        applyFullInvisibility(player);
    }

    /** Perfect Echo (and the invisible phase of Distorted Echo / Lost Signal): stays hidden, armor included. */
    private void applyFullInvisibility(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, INVISIBILITY_EFFECT_TICKS, 0, true, false, false
        ));
        EchoPacketService.hideEquipment(player); // true invisibility: hide worn armor too, not just the body
    }

    private void applyNametagVisibility(Player player, EchoTier tier) {
        boolean shouldHide = tier == EchoTier.DISTORTED_ECHO || tier == EchoTier.LOST_SIGNAL || tier == EchoTier.PERFECT_ECHO;
        boolean currentlyHidden = nametagHidden.contains(player.getUniqueId());

        if (shouldHide && !currentlyHidden) {
            Team team = getOrCreateHiddenTagTeam();
            if (team != null) {
                team.addEntry(player.getName());
                nametagHidden.add(player.getUniqueId());
            }
        } else if (!shouldHide && currentlyHidden) {
            Team team = getOrCreateHiddenTagTeam();
            if (team != null) {
                team.removeEntry(player.getName());
            }
            nametagHidden.remove(player.getUniqueId());
        }
    }

    /**
     * Approximation: Bukkit has no "quieter footsteps" API, only fully
     * silent or not. setSilent(true) also suppresses hurt/idle sounds,
     * not just footsteps - flagged as a known limitation.
     */
    private void applyFootstepSilence(Player player, EchoTier tier) {
        boolean shouldSilence = tier == EchoTier.DISTORTED_ECHO || tier == EchoTier.LOST_SIGNAL || tier == EchoTier.PERFECT_ECHO;
        boolean currentlySilenced = footstepsSilenced.contains(player.getUniqueId());

        if (shouldSilence && !currentlySilenced) {
            player.setSilent(true);
            footstepsSilenced.add(player.getUniqueId());
        } else if (!shouldSilence && currentlySilenced) {
            player.setSilent(false);
            footstepsSilenced.remove(player.getUniqueId());
        }
    }

    /**
     * Lost Signal only: fake footstep particles trailing near the player as
     * they move, so onlookers can't tell where the real footsteps are.
     */
    private void applyAmbientIllusions(Player player, EchoTier tier) {
        if (tier != EchoTier.LOST_SIGNAL) return;

        boolean isMoving = player.getVelocity().lengthSquared() > 0.001;
        if (!isMoving) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (random.nextDouble() < ILLUSION_FOOTSTEP_CHANCE_PER_RUN) {
            Location fake = player.getLocation().add(
                    random.nextDouble(-1.2, 1.2), 0.05, random.nextDouble(-1.2, 1.2)
            );
            player.getWorld().spawnParticle(Particle.SCULK_SOUL, fake, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Perfect Echo's "you're stronger" side: faster attacks via an
     * Attribute modifier (bonus damage itself is applied in
     * EchoCombatListener, since that's per-hit not passive).
     */
    private void applyPerfectEchoBuffs(Player player, EchoTier tier) {
        boolean shouldBuff = tier == EchoTier.PERFECT_ECHO;
        boolean currentlyBuffed = attackSpeedBuffed.contains(player.getUniqueId());

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed == null) return;

        if (shouldBuff && !currentlyBuffed) {
            attackSpeed.addModifier(new AttributeModifier(
                    PERFECT_ECHO_ATTACK_SPEED_KEY, PERFECT_ECHO_ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADD_SCALAR
            ));
            attackSpeedBuffed.add(player.getUniqueId());
        } else if (!shouldBuff && currentlyBuffed) {
            removeAttackSpeedModifier(attackSpeed);
            attackSpeedBuffed.remove(player.getUniqueId());
        }
    }

    private void removeAttackSpeedModifier(AttributeInstance attackSpeed) {
        attackSpeed.getModifiers().stream()
                .filter(modifier -> modifier.getKey().equals(PERFECT_ECHO_ATTACK_SPEED_KEY))
                .forEach(attackSpeed::removeModifier);
    }

    private void cleanupIfNeeded(Player player) {
        revealedUntilMillis.remove(player.getUniqueId());
        lastLostSignalPulseAt.remove(player.getUniqueId());
        EchoPacketService.revealEquipment(player); // in case they left mid-invis with armor hidden from viewers
        if (nametagHidden.remove(player.getUniqueId())) {
            Team team = getOrCreateHiddenTagTeam();
            if (team != null) team.removeEntry(player.getName());
        }
        if (footstepsSilenced.remove(player.getUniqueId())) {
            player.setSilent(false);
        }
        if (attackSpeedBuffed.remove(player.getUniqueId())) {
            AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) removeAttackSpeedModifier(attackSpeed);
        }
    }

    private Team getOrCreateHiddenTagTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return null;

        Scoreboard scoreboard = manager.getMainScoreboard();
        Team team = scoreboard.getTeam(HIDDEN_TAG_TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(HIDDEN_TAG_TEAM_NAME);
            team.setColor(ChatColor.GRAY);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
        return team;
    }
}