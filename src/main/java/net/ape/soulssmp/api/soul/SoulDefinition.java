package net.ape.soulssmp.api.soul;

import net.ape.soulssmp.api.Ability;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.managers.core.AbilityManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for a Soul's content: its abilities, ultimate,
 * passive tasks, and /soul info text. Built once via {@link Builder} in
 * net.ape.soulssmp.api.soul.SoulDefinitions - that's the one place to touch
 * when adding or changing a Soul.
 */
public final class SoulDefinition {

    public record PassiveSchedule(BukkitRunnable task, long delay, long period) {
    }

    private final SoulType type;
    private final String displayName;
    private final String description;
    private final AbilityType ability1;
    private final AbilityType ability2;
    private final AbilityType ultimate;
    private final List<PassiveSchedule> passives;
    private final List<String> infoLines;

    private SoulDefinition(Builder builder) {
        this.type = builder.type;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.ability1 = builder.ability1;
        this.ability2 = builder.ability2;
        this.ultimate = builder.ultimate;
        this.passives = List.copyOf(builder.passives);
        this.infoLines = List.copyOf(builder.infoLines);
    }

    public SoulType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public AbilityType getAbility1() {
        return ability1;
    }

    public AbilityType getAbility2() {
        return ability2;
    }

    public AbilityType getUltimate() {
        return ultimate;
    }

    public List<PassiveSchedule> getPassives() {
        return passives;
    }

    public List<String> getInfoLines() {
        return infoLines;
    }

    public static Builder builder(SoulType type, String displayName, String description) {
        return new Builder(type, displayName, description);
    }

    public static final class Builder {

        private final SoulType type;
        private final String displayName;
        private final String description;
        private AbilityType ability1;
        private AbilityType ability2;
        private AbilityType ultimate;
        private final List<PassiveSchedule> passives = new ArrayList<>();
        private List<String> infoLines = List.of();

        private Builder(SoulType type, String displayName, String description) {
            this.type = type;
            this.displayName = displayName;
            this.description = description;
        }

        public Builder ability1(Ability instance, AbilityManager abilityManager) {
            abilityManager.registerAbility(instance);
            this.ability1 = instance.getType();
            return this;
        }

        public Builder ability1(AbilityType typeOnly) {
            this.ability1 = typeOnly;
            return this;
        }

        public Builder ability2(Ability instance, AbilityManager abilityManager) {
            abilityManager.registerAbility(instance);
            this.ability2 = instance.getType();
            return this;
        }

        public Builder ability2(AbilityType typeOnly) {
            this.ability2 = typeOnly;
            return this;
        }

        public Builder ultimate(Ability instance, AbilityManager abilityManager) {
            abilityManager.registerAbility(instance);
            this.ultimate = instance.getType();
            return this;
        }

        public Builder ultimate(AbilityType typeOnly) {
            this.ultimate = typeOnly;
            return this;
        }

        public Builder passive(BukkitRunnable task, long delay, long period) {
            this.passives.add(new PassiveSchedule(task, delay, period));
            return this;
        }

        public Builder infoLines(List<String> lines) {
            this.infoLines = lines;
            return this;
        }

        public SoulDefinition build() {
            return new SoulDefinition(this);
        }
    }
}
