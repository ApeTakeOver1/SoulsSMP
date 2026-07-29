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
            player.sendTitle("Ability", "That ability isn't available yet.", 0, 40, 10);
            return;
        }

        PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);
        if (data.getSoul() != ability.getRequiredSoul()) {
            player.sendTitle("Ability", ChatColor.DARK_GRAY + "" + ChatColor.BOLD + ability.getDisplayName() + " doesn't belong to your Soul.", 0, 40, 10);
            return;
        }

        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        boolean hasCooldown = ability.getCooldownSeconds() > 0;

        if (!isCreative && hasCooldown) {
            long remaining = getRemainingCooldownSeconds(player, type);
            if (remaining > 0) {
                player.sendTitle("Ability Cooldown", ChatColor.DARK_GRAY + "" + ChatColor.BOLD + ability.getDisplayName() + " is on cooldown (" + remaining + "s).", 0, 40, 10);
                return;
            }
        }

        boolean fullUse = ability.execute(player);

        if (fullUse && !isCreative && hasCooldown) {
            setCooldown(player, type, ability.getCooldownSeconds());
        }
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