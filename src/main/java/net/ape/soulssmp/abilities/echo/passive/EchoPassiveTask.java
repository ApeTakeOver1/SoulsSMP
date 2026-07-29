package net.ape.soulssmp.abilities.echo.passive;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.abilities.echo.EchoTier;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles Echo Soul's passive: baseline Speed, the Noise Meter's tier-based
 * distortion effects (flicker "transparency", quiet footsteps, hidden
 * nametag), and ambient illusion sounds/particles.
 *
 * Runs every 5 ticks (0.25s) so the flicker cycle and nametag/silence state
 * stay responsive.
 *
 * PLACEHOLDER TUNING - flicker duty-cycle fractions per tier, ambient
 * illusion chances per run - all easy to retune once tested in-game.
 */
public class EchoPassiveTask extends BukkitRunnable {

    private static final String HIDDEN_TAG_TEAM_NAME = "soulssmp_echo_hidden";

    private static final int CYCLE_RUNS = 8; // 8 runs * 5 ticks = 2 seconds per flicker cycle
    private static final int INVISIBILITY_EFFECT_TICKS = 7; // slightly longer than the 5-tick run gap

    // How much of the flicker cycle is spent INVISIBLE, per tier. 1.0 = always invisible.
    private static final double DISTORTED_ECHO_INVISIBLE_FRACTION = 0.15;
    private static final double LOST_SIGNAL_INVISIBLE_FRACTION = 0.5;

    private static final double ILLUSION_FOOTSTEP_CHANCE_PER_RUN = 0.20; // Lost Signal+
    private static final double FAKE_SWING_CHANCE_PER_RUN = 0.05; // Lost Signal only

    private final Set<UUID> nametagHidden = new HashSet<>();
    private final Set<UUID> footstepsSilenced = new HashSet<>();

    // Players temporarily forced visible (e.g. Perfect Echo's "revealed on attack").
    // Public so EchoCombatListener can push into it.
    private static final Map<UUID, Long> revealedUntilMillis = new HashMap<>();

    private int tickCounter = 0;

    public static void revealBriefly(Player player, long durationMillis) {
        revealedUntilMillis.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    private static boolean isCurrentlyRevealed(Player player) {
        Long until = revealedUntilMillis.get(player.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            revealedUntilMillis.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

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
                applyFlicker(player, tier);
                applyNametagVisibility(player, tier);
                applyFootstepSilence(player, tier);
                applyAmbientIllusions(player, tier);
            } catch (Exception e) {
                SoulsSMP.getInstance().getLogger().warning(
                        "EchoPassiveTask error for " + player.getName() + ": " + e.getMessage()
                );
            }
        }
    }

    private void applySpeed(Player player, EchoTier tier) {
        int amplifier = (tier == EchoTier.DORMANT) ? 1 : 2; // Speed II baseline, Speed III once resonance > 0
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, amplifier, false, true, true));
    }

    private void applyFlicker(Player player, EchoTier tier) {
        if (isCurrentlyRevealed(player)) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            return;
        }

        double invisibleFraction;
        switch (tier) {
            case DISTORTED_ECHO -> invisibleFraction = DISTORTED_ECHO_INVISIBLE_FRACTION;
            case LOST_SIGNAL -> invisibleFraction = LOST_SIGNAL_INVISIBLE_FRACTION;
            case PERFECT_ECHO -> invisibleFraction = 1.0;
            default -> invisibleFraction = 0.0;
        }

        if (invisibleFraction <= 0) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            return;
        }

        int invisibleRuns = (int) Math.round(CYCLE_RUNS * invisibleFraction);
        boolean shouldBeInvisibleThisRun = (tickCounter % CYCLE_RUNS) < invisibleRuns;

        if (shouldBeInvisibleThisRun) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY, INVISIBILITY_EFFECT_TICKS, 0, true, false, false
            ));
        } else if (tier != EchoTier.PERFECT_ECHO) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
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

    private void applyAmbientIllusions(Player player, EchoTier tier) {
        if (tier != EchoTier.LOST_SIGNAL && tier != EchoTier.PERFECT_ECHO) return;

        boolean isMoving = player.getVelocity().lengthSquared() > 0.001;
        if (!isMoving) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (random.nextDouble() < ILLUSION_FOOTSTEP_CHANCE_PER_RUN) {
            Location fake = player.getLocation().add(
                    random.nextDouble(-1.2, 1.2), 0.05, random.nextDouble(-1.2, 1.2)
            );
            player.getWorld().spawnParticle(Particle.SCULK_SOUL, fake, 1, 0, 0, 0, 0);
        }

        if (tier == EchoTier.LOST_SIGNAL && random.nextDouble() < FAKE_SWING_CHANCE_PER_RUN) {
            Location fake = player.getLocation().add(
                    random.nextDouble(-3, 3), 1, random.nextDouble(-3, 3)
            );
            player.getWorld().playSound(fake, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.2f);
        }
    }

    private void cleanupIfNeeded(Player player) {
        if (nametagHidden.remove(player.getUniqueId())) {
            Team team = getOrCreateHiddenTagTeam();
            if (team != null) team.removeEntry(player.getName());
        }
        if (footstepsSilenced.remove(player.getUniqueId())) {
            player.setSilent(false);
        }
        revealedUntilMillis.remove(player.getUniqueId());
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
