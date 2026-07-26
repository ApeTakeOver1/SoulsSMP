package net.ape.soulssmp.ability;

import org.bukkit.entity.Player;

public abstract class Ability {

    private final AbilityType type;
    private final String displayName;
    private final int manaCost;
    private final int cooldownSeconds;

    public Ability(AbilityType type, String displayName, int manaCost, int cooldownSeconds) {
        this.type = type;
        this.displayName = displayName;
        this.manaCost = manaCost;
        this.cooldownSeconds = cooldownSeconds;
    }

    public AbilityType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getManaCost() {
        return manaCost;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * Runs the ability.
     * @return true if this counted as a full use and should start the cooldown,
     *         false if nothing actually happened (failed cast, arming phase, etc.)
     */
    public abstract boolean execute(Player player);
}