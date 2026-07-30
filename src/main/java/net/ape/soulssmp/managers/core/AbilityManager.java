package net.ape.soulssmp.managers.core;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.Ability;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityManager {

    private static final long ACTION_BAR_FEEDBACK_MILLIS = 2500L;

    private final Map<AbilityType, Ability> registeredAbilities = new HashMap<>();
    private final Map<UUID, Map<AbilityType, Long>> cooldowns = new HashMap<>();

    public void registerAbility(Ability ability) {
        registeredAbilities.put(ability.getType(), ability);
    }

    public Ability getAbility(AbilityType type) {
        return registeredAbilities.get(type);
    }

    public <T extends Ability> T getAs(AbilityType type, Class<T> clazz) {
        return clazz.cast(registeredAbilities.get(type));
    }

    public void useAbility(Player player, AbilityType type) {
        Ability ability = registeredAbilities.get(type);

        if (ability == null) {
            showFeedback(player, ChatColor.GRAY + "That ability isn't available yet.");
            return;
        }

        PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);
        if (data.getSoul() != ability.getRequiredSoul()) {
            showFeedback(player, ChatColor.DARK_GRAY + "" + ChatColor.BOLD + ability.getDisplayName()
                    + ChatColor.RED + " doesn't belong to your Soul.");
            return;
        }

        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        boolean hasCooldown = ability.getCooldownSeconds() > 0;

        if (!isCreative && hasCooldown) {
            long remaining = getRemainingCooldownSeconds(player, type);
            if (remaining > 0) {
                showFeedback(player, ChatColor.DARK_GRAY + "" + ChatColor.BOLD + ability.getDisplayName()
                        + ChatColor.RED + " is on cooldown (" + remaining + "s).");
                return;
            }
        }

        boolean fullUse = ability.execute(player);

        if (fullUse && !isCreative && hasCooldown) {
            setCooldown(player, type, ability.getCooldownSeconds());
        }
    }

    private void showFeedback(Player player, String message) {
        SoulsSMP.getInstance().getHudManager()
                .showActionBarOverride(player, "§8§lSoul §7» " + message, ACTION_BAR_FEEDBACK_MILLIS);
    }

    public long getRemainingCooldownSeconds(Player player, AbilityType type) {
        if (player.getGameMode() == GameMode.CREATIVE) return 0;

        Map<AbilityType, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long readyAt = playerCooldowns.get(type);
        if (readyAt == null) return 0;

        long remainingMillis = readyAt - System.currentTimeMillis();
        return remainingMillis > 0 ? (remainingMillis / 1000) + 1 : 0;
    }

    private void setCooldown(Player player, AbilityType type, int cooldownSeconds) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(type, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    public void reduceCooldown(Player player, AbilityType type, int seconds) {
        Map<AbilityType, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return;

        Long readyAt = playerCooldowns.get(type);
        if (readyAt == null) return;

        long newReadyAt = readyAt - (seconds * 1000L);
        long now = System.currentTimeMillis();

        if (newReadyAt <= now) {
            playerCooldowns.remove(type);
        } else {
            playerCooldowns.put(type, newReadyAt);
        }
    }
}
