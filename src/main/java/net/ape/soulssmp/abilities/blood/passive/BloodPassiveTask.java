package net.ape.soulssmp.abilities.blood.passive;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.data.PlayerData;
import net.ape.soulssmp.api.SoulType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
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

public class BloodPassiveTask extends BukkitRunnable {

    private static final double LOW_HEALTH_THRESHOLD = 0.40;
    private static final double SENSE_THRESHOLD = 0.25;
    private static final double SENSE_RADIUS = 10.0; // shrunk from 15 - this IS the real detection range

    private static final NamespacedKey ATTACK_SPEED_KEY =
            new NamespacedKey(SoulsSMP.getInstance(), "blood_pact_attack_speed");

    private static final Set<UUID> pactActive = new HashSet<>();

    // Blood Sense tracking (formerly BloodSenseManager) - only this task uses it.
    private static final String SENSE_TEAM_NAME = "soulssmp_blood_glow";
    private final Map<UUID, Set<UUID>> sensedBy = new HashMap<>();
    private final Map<UUID, Set<UUID>> sensingTargets = new HashMap<>();

    private int tickCounter = 0;

    @Override
    public void run() {
        tickCounter++;
        boolean playHeartbeatThisTick = tickCounter % 2 == 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Debuff visuals run for EVERYONE, not just Blood soul owners - these are
            // effects Curse/Silence inflict on a target, who may have any soul (or none).
            handleDebuffVisuals(player);

            PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);

            if (data.getSoul() != SoulType.BLOOD) {
                if (pactActive.contains(player.getUniqueId())) {
                    removePact(player);
                }
                continue;
            }

            try {
                handleBloodPact(player);
                handleBloodSense(player, playHeartbeatThisTick);
            } catch (Exception e) {
                SoulsSMP.getInstance().getLogger().warning(
                        "BloodPassiveTask error for " + player.getName() + ": " + e.getMessage()
                );
            }
        }
    }

    /**
     * Continuous particle confirmation for debuffs with no natural vanilla
     * visual (formerly BloodDebuffVisualTask) - Silence has zero built-in
     * indicator at all, and Restriction's Slowness/no-jump is easy to miss.
     * Without this, a player genuinely cannot tell these effects are active.
     */
    private void handleDebuffVisuals(Player player) {
        if (SoulsSMP.getInstance().getSilenceManager().isSilenced(player)) {
            player.getWorld().spawnParticle(
                    Particle.DUST, player.getLocation().add(0, 2.2, 0), 2, 0.2, 0.1, 0.2, 0,
                    new Particle.DustOptions(Color.fromRGB(120, 0, 150), 1.0f)
            );
        }

        if (SoulsSMP.getInstance().getCurse().isRestricted(player)) {
            player.getWorld().spawnParticle(
                    Particle.DUST, player.getLocation().add(0, 0.1, 0), 4, 0.3, 0.05, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 30, 30), 1.0f)
            );
        }
    }

    private void handleBloodPact(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double healthPercent = player.getHealth() / maxHealth;

        if (healthPercent <= LOW_HEALTH_THRESHOLD) {
            applyPact(player);
        } else {
            removePact(player);
        }
    }

    private void applyPact(Player player) {
        boolean wasActive = pactActive.contains(player.getUniqueId());
        pactActive.add(player.getUniqueId());

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, 1, false, true, true));

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null && attackSpeed.getModifier(ATTACK_SPEED_KEY) == null) {
            attackSpeed.addModifier(new AttributeModifier(
                    ATTACK_SPEED_KEY, 0.5, AttributeModifier.Operation.ADD_NUMBER
            ));
        }

        if (!wasActive) {
            player.sendMessage("§4§lBlood §7» §cYour Blood Pact awakens.");
        }
    }

    private void removePact(Player player) {
        boolean wasActive = pactActive.remove(player.getUniqueId());

        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) {
            AttributeModifier existing = attackSpeed.getModifier(ATTACK_SPEED_KEY);
            if (existing != null) {
                attackSpeed.removeModifier(existing);
            }
        }

        if (wasActive) {
            player.sendMessage("§4§lBlood §7» §7Your Blood Pact fades.");
        }
    }

    public static boolean isPactActive(Player player) {
        return pactActive.contains(player.getUniqueId());
    }

    private void handleBloodSense(Player observer, boolean playHeartbeat) {
        // STEP 1: re-check every target we're CURRENTLY sensing, even if
        // they're no longer nearby right now - this is what actually turns
        // the glow off when they leave range or heal up.
        for (UUID targetId : getCurrentlySensedTargets(observer.getUniqueId())) {
            Player target = Bukkit.getPlayer(targetId);

            if (target == null || !target.isOnline()) {
                stopSensing(observer.getUniqueId(), targetId);
                continue;
            }

            double distance = target.getLocation().distance(observer.getLocation());
            double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            double healthPercent = target.getHealth() / maxHealth;

            boolean stillQualifies = distance <= SENSE_RADIUS && healthPercent <= SENSE_THRESHOLD;

            if (!stillQualifies) {
                stopSensing(observer.getUniqueId(), targetId);
                continue;
            }

            target.getWorld().spawnParticle(
                    Particle.DUST, target.getLocation().add(0, 1, 0),
                    3, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f)
            );

            if (playHeartbeat) {
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.5f);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 1.5f);
            }
        }

        // STEP 2: scan for NEW qualifying targets not already being tracked.
        for (Entity nearby : observer.getNearbyEntities(SENSE_RADIUS, SENSE_RADIUS, SENSE_RADIUS)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.getUniqueId().equals(observer.getUniqueId())) continue;

            double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            double healthPercent = target.getHealth() / maxHealth;

            if (healthPercent <= SENSE_THRESHOLD) {
                startSensing(observer.getUniqueId(), target.getUniqueId());
            }
        }

        if (isObserverSensingAnything(observer.getUniqueId())) {
            drawDetectionRing(observer);
        }
    }

    /**
     * Denser ring: smaller radius (10 blocks) and particles every 3 degrees
     * instead of every 10, for a noticeably thicker/more visible circle.
     */
    private void drawDetectionRing(Player observer) {
        Location center = observer.getLocation();

        for (int i = 0; i < 360; i += 3) {
            double rad = Math.toRadians(i);
            double x = center.getX() + SENSE_RADIUS * Math.cos(rad);
            double z = center.getZ() + SENSE_RADIUS * Math.sin(rad);
            Location edge = new Location(center.getWorld(), x, center.getY() + 0.2, z);

            center.getWorld().spawnParticle(
                    Particle.DUST, edge, 2, 0, 0.15, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(220, 0, 0), 1.3f)
            );
        }
    }

    private void startSensing(UUID observerId, UUID targetId) {
        Set<UUID> observers = sensedBy.computeIfAbsent(targetId, k -> new HashSet<>());
        boolean wasEmpty = observers.isEmpty();
        observers.add(observerId);

        if (wasEmpty) {
            applyGlow(targetId);
        }

        sensingTargets.computeIfAbsent(observerId, k -> new HashSet<>()).add(targetId);
    }

    private void stopSensing(UUID observerId, UUID targetId) {
        Set<UUID> observers = sensedBy.get(targetId);
        if (observers != null) {
            observers.remove(observerId);
            if (observers.isEmpty()) {
                sensedBy.remove(targetId);
                removeGlow(targetId);
            }
        }

        Set<UUID> targets = sensingTargets.get(observerId);
        if (targets != null) {
            targets.remove(targetId);
            if (targets.isEmpty()) {
                sensingTargets.remove(observerId);
            }
        }
    }

    /**
     * Returns a COPY of the target IDs this observer is currently sensing.
     * A copy is required because the loop above needs to safely check each
     * one (and potentially call stopSensing, which mutates the real set)
     * without a ConcurrentModificationException.
     */
    private Set<UUID> getCurrentlySensedTargets(UUID observerId) {
        Set<UUID> targets = sensingTargets.get(observerId);
        return targets == null ? new HashSet<>() : new HashSet<>(targets);
    }

    private boolean isObserverSensingAnything(UUID observerId) {
        Set<UUID> targets = sensingTargets.get(observerId);
        return targets != null && !targets.isEmpty();
    }

    private void applyGlow(UUID targetId) {
        var player = Bukkit.getPlayer(targetId);
        if (player == null) return;

        Team team = getOrCreateSenseTeam();
        if (team != null) {
            team.addEntry(player.getName());
        }

        player.setGlowing(true);
    }

    private void removeGlow(UUID targetId) {
        var player = Bukkit.getPlayer(targetId);
        if (player == null) return;

        Team team = getOrCreateSenseTeam();
        if (team != null) {
            team.removeEntry(player.getName());
        }

        player.setGlowing(false);
    }

    public void setupSenseTeam() {
        getOrCreateSenseTeam();
    }

    private Team getOrCreateSenseTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return null;

        Scoreboard scoreboard = manager.getMainScoreboard();
        Team team = scoreboard.getTeam(SENSE_TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(SENSE_TEAM_NAME);
            team.setColor(ChatColor.RED);
        }
        return team;
    }
}