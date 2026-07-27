package net.ape.soulssmp.soul.types.bloodsoul;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BloodSenseManager {

    private static final String TEAM_NAME = "soulssmp_blood_glow";

    private final Map<UUID, Set<UUID>> sensedBy = new HashMap<>();
    private final Map<UUID, Set<UUID>> sensingTargets = new HashMap<>();

    public void setupTeam() {
        Scoreboard scoreboard = getMainScoreboardSafely();
        if (scoreboard == null) return;

        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
            team.setColor(org.bukkit.ChatColor.RED);
        }
    }

    private Scoreboard getMainScoreboardSafely() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return null;
        return manager.getMainScoreboard();
    }

    private Team getOrCreateTeam() {
        Scoreboard scoreboard = getMainScoreboardSafely();
        if (scoreboard == null) return null;

        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
            team.setColor(org.bukkit.ChatColor.RED);
        }
        return team;
    }

    public void startSensing(UUID observerId, UUID targetId) {
        Set<UUID> observers = sensedBy.computeIfAbsent(targetId, k -> new HashSet<>());
        boolean wasEmpty = observers.isEmpty();
        observers.add(observerId);

        if (wasEmpty) {
            applyGlow(targetId);
        }

        sensingTargets.computeIfAbsent(observerId, k -> new HashSet<>()).add(targetId);
    }

    public void stopSensing(UUID observerId, UUID targetId) {
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
     * A copy is required because the task loop needs to safely check each
     * one (and potentially call stopSensing, which mutates the real set)
     * without a ConcurrentModificationException.
     */
    public Set<UUID> getCurrentlySensedTargets(UUID observerId) {
        Set<UUID> targets = sensingTargets.get(observerId);
        return targets == null ? new HashSet<>() : new HashSet<>(targets);
    }

    public boolean isObserverSensingAnything(UUID observerId) {
        Set<UUID> targets = sensingTargets.get(observerId);
        return targets != null && !targets.isEmpty();
    }

    private void applyGlow(UUID targetId) {
        var player = Bukkit.getPlayer(targetId);
        if (player == null) return;

        Team team = getOrCreateTeam();
        if (team != null) {
            team.addEntry(player.getName());
        }

        player.setGlowing(true);
    }

    private void removeGlow(UUID targetId) {
        var player = Bukkit.getPlayer(targetId);
        if (player == null) return;

        Team team = getOrCreateTeam();
        if (team != null) {
            team.removeEntry(player.getName());
        }

        player.setGlowing(false);
    }
}