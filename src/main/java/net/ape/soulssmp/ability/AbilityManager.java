package net.ape.soulssmp.ability;

import org.bukkit.entity.Player;

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

    /**
     * Attempts to use an ability. Handles cooldown checking/setting.
     * Mana checking happens inside the ability itself.
     */
    public boolean useAbility(Player player, AbilityType type) {
        Ability ability = registeredAbilities.get(type);

        if (ability == null) {
            player.sendMessage("§8§lVoid §7» §cThat ability isn't available yet.");
            return false;
        }

        long remaining = getRemainingCooldownSeconds(player, type);
        if (remaining > 0) {
            player.sendMessage("§8§lVoid §7» §c" + ability.getDisplayName() + " is on cooldown (" + remaining + "s).");
            return false;
        }

        boolean fullUse = ability.execute(player);

        if (fullUse) {
            setCooldown(player, type, ability.getCooldownSeconds());
        }

        return fullUse;
    }

    public long getRemainingCooldownSeconds(Player player, AbilityType type) {
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
}