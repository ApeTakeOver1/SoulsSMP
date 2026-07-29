package net.ape.soulssmp.api;

import net.ape.soulssmp.api.SoulType;
import org.bukkit.entity.Player;

public abstract class Ability {

    private final AbilityType type;
    private final String displayName;
    private final int manaCost;
    private final int cooldownSeconds;
    private final SoulType requiredSoul;

    public Ability(AbilityType type, String displayName, int manaCost, int cooldownSeconds, SoulType requiredSoul) {
        this.type = type;
        this.displayName = displayName;
        this.manaCost = manaCost;
        this.cooldownSeconds = cooldownSeconds;
        this.requiredSoul = requiredSoul;
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

    public SoulType getRequiredSoul() {
        return requiredSoul;
    }

    public abstract boolean execute(Player player);
}