package net.ape.soulssmp.api.soul;

import net.ape.soulssmp.abilities.blood.active.Curse;
import net.ape.soulssmp.abilities.blood.active.Hemorrhage;
import net.ape.soulssmp.abilities.blood.ultimate.BloodHands;
import net.ape.soulssmp.abilities.blood.passive.BloodPassiveTask;
import net.ape.soulssmp.abilities.echo.passive.EchoPassiveTask;
import net.ape.soulssmp.abilities.voidsoul.active.AbyssMark;
import net.ape.soulssmp.abilities.voidsoul.active.VoidStep;
import net.ape.soulssmp.abilities.voidsoul.ultimate.NullField;
import net.ape.soulssmp.abilities.voidsoul.passive.VoidPassiveTask;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.managers.core.AbilityManager;
import net.ape.soulssmp.tasks.ResonanceTask;

import java.util.List;

/**
 * Single wiring point for every Soul's abilities, ultimate, passives, and
 * /soul info text. To add a new Soul: build its Ability/passive instances
 * where the rest of the plugin's souls are built, then add one
 * registry.register(SoulDefinition.builder(...)...) block here.
 */
public final class SoulDefinitions {

    private SoulDefinitions() {
    }

    public static SoulRegistry build(AbilityManager abilityManager,
                                     VoidStep voidStep, AbyssMark abyssMark, NullField nullField, VoidPassiveTask voidPassiveTask,
                                     Curse curse, Hemorrhage hemorrhage, BloodHands bloodHands, BloodPassiveTask bloodPassiveTask,
                                     EchoPassiveTask echoPassiveTask, ResonanceTask resonanceTask) {
        SoulRegistry registry = new SoulRegistry();

        registry.register(SoulDefinition.builder(SoulType.VOID, "Void Soul", "Space, emptiness, decay")
                .ability1(voidStep, abilityManager)
                .ability2(abyssMark, abilityManager)
                .ultimate(nullField, abilityManager)
                .passive(voidPassiveTask, 0L, 10L)
                .infoLines(List.of(
                        "§5§l✦ Void Soul §7— §7Space, emptiness, decay",
                        "",
                        "§f§lPassive:",
                        "§7 - Resistance below 30% health",
                        "§7 - Drains nearby enemy mana while you're at full HP",
                        "",
                        "§f§lAbility 1 §7(/soul ability1) - Void Step:",
                        "§7 Arms a short dash, use again to teleport + slash everything in the path",
                        "",
                        "§f§lAbility 2 §7(/soul ability2) - Abyss Mark:",
                        "§7 Marks a target - builds a stack on every hit, caps at 5",
                        "",
                        "§f§lNull Field §7(Sneak + F, Ultimate):",
                        "§7 Instant burst - weakens caught enemies for 45s"
                ))
                .build());

        registry.register(SoulDefinition.builder(SoulType.BLOOD, "Blood Soul", "Sacrifice, control, health")
                .ability1(curse, abilityManager)
                .ability2(hemorrhage, abilityManager)
                .ultimate(bloodHands, abilityManager)
                .passive(bloodPassiveTask, 0L, 10L)
                .infoLines(List.of(
                        "§4§l✦ Blood Soul §7— §7Sacrifice, control, health",
                        "",
                        "§f§lPassives:",
                        "§7 - Blood Pact: below 40% HP, gain speed, attack speed, lifesteal",
                        "§7 - Blood Sense: detect low-HP enemies within 10 blocks",
                        "",
                        "§f§lAbility 1 §7(/soul ability1) - Curse:",
                        "§7 3s channel, then locks target - choose a debuff",
                        "",
                        "§f§lAbility 2 §7(/soul ability2) - Hemorrhage:",
                        "§7 Mark that builds stacks - ruptures at 5 hits for 5 hearts true damage",
                        "",
                        "§f§lBlood Hands / Puncture §7(Sneak + F, Ultimate):",
                        "§7 Above 5 hearts: Blood Hands (costs HP). At/below 5 hearts: Puncture (free)"
                ))
                .build());

        registry.register(SoulDefinition.builder(SoulType.ECHO, "Echo Soul", "Confusion, trickery, deception")
                .ability1(AbilityType.FALSE_REFLECTION)
                .ability2(AbilityType.VANISHING_STRIKE)
                .ultimate(AbilityType.ABSOLUTE_ECHO)
                .passive(echoPassiveTask, 0L, 5L)
                .passive(resonanceTask, 20L, 20L)
                .infoLines(List.of(
                        "§f§l✦ Echo Soul §7— §7Confusion, trickery, deception",
                        "",
                        "§f§lNo Mana §7— runs on the Noise Meter (Resonance) instead",
                        "§7 - Speed II always, Speed III once Resonance rises above 0%",
                        "§7 - Immune to Warden targeting and Sonic Boom damage",
                        "§7 - Resonance rises from combat and nearby explosions, drains when out of combat",
                        "§7 - §f0-25% Echo§7: extra Resonance from attacking players or mobs",
                        "§7 - §f25-50% Distorted Echo§7: slight transparency, quiet footsteps, hidden nametag",
                        "§7 - §f50-75% Lost Signal§7: 50% transparency, illusion footsteps as you move",
                        "§7 - §f75-100% Perfect Echo§7: faster attacks, more damage, afterimage on every hit",
                        "",
                        "§7§oAbilities (False Reflection, Vanishing Strike, Absolute Echo) are not implemented yet."
                ))
                .build());

        return registry;
    }
}