package net.ape.soulssmp.commands;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.data.PlayerData;
import net.ape.soulssmp.api.SoulType;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SoulCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            showSelfInfo(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("ability1")) {
            useMappedAbility(player, 1);
            return true;
        }

        if (args[0].equalsIgnoreCase("ability2")) {
            useMappedAbility(player, 2);
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            handleInfo(player, args);
            return true;
        }

        player.sendMessage("§8§lSoul §7» §cUnknown subcommand. Try §f/soul§7, §f/soul info§7, §f/soul ability1§7, or §f/soul ability2");
        return true;
    }

    private void useMappedAbility(Player player, int slot) {
        PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);
        AbilityType type;

        if (data.getSoul() == SoulType.VOID) {
            type = slot == 1 ? AbilityType.VOID_STEP : AbilityType.ABYSS_MARK;
        } else if (data.getSoul() == SoulType.BLOOD) {
            type = slot == 1 ? AbilityType.CURSE : AbilityType.HEMORRHAGE;
        } else if (data.getSoul() == SoulType.ECHO) {
            type = slot == 1 ? AbilityType.FALSE_REFLECTION : AbilityType.VANISHING_STRIKE;
        } else {
            player.sendMessage("§8§lSoul §7» §cYou have no Soul bound.");
            return;
        }

        SoulsSMP.getInstance().getAbilityManager().useAbility(player, type);
    }

    private void showSelfInfo(Player player) {
        PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);
        boolean creative = player.getGameMode() == GameMode.CREATIVE;

        player.sendMessage("§8§m----------------------------------");
        player.sendMessage("§5§l✦ Your Soul");

        if (data.getSoul() == null) {
            player.sendMessage("§7You have not been bound to a Soul yet.");
        } else {
            player.sendMessage("§7Soul: §f" + formatSoulName(data.getSoul()));

            if (data.getSoul() == SoulType.ECHO) {
                double resonance = SoulsSMP.getInstance().getResonanceManager().getResonance(player);
                player.sendMessage("§7Resonance: §f" + (creative ? "∞" : (int) resonance + "% §7(" +
                        SoulsSMP.getInstance().getResonanceManager().getTier(player).getDisplayName() + "§7)"));
            } else {
                player.sendMessage("§7Mana: §f" + (creative ? "∞" : data.getMana() + " / " + data.getMaxMana()));
            }

            player.sendMessage("§7Favor: §f" + data.getFavor() + " §7(-5 to +5)");
            player.sendMessage("§7Resurrections: §f" + data.getResurrectionCount());
            player.sendMessage("§7Awakened Weapon: §f" + (data.isAwakened() ? "Yes" : "No"));
        }

        if (creative) {
            player.sendMessage("§7§oCreative Mode: infinite mana, no cooldowns.");
        }

        player.sendMessage("§8§m----------------------------------");
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length > 1) {
            showSoulDetail(player, args[1]);
            return;
        }

        player.sendMessage("§8§m----------------------------------");
        player.sendMessage("§5§l✦ The Souls");
        player.sendMessage("§7Use §f/soul info <name>§7 for details.");
        player.sendMessage("");

        for (SoulType type : SoulType.values()) {
            String status = type == SoulType.ECHO ? "§e(passive live, abilities in progress)" : "§a(available)";
            player.sendMessage("§7 - §f" + formatSoulName(type) + " " + status);
        }

        player.sendMessage("§8§m----------------------------------");
    }

    private void showSoulDetail(Player player, String soulArg) {
        SoulType type;
        try {
            type = SoulType.valueOf(soulArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§8§lSoul §7» §cUnknown soul: " + soulArg);
            return;
        }

        if (type == SoulType.VOID) {
            showVoidDetail(player);
        } else if (type == SoulType.BLOOD) {
            showBloodDetail(player);
        } else if (type == SoulType.ECHO) {
            showEchoDetail(player);
        }
    }

    private void showEchoDetail(Player player) {
        player.sendMessage("§8§m----------------------------------");
        player.sendMessage("§f§l✦ Echo Soul §7— §7Confusion, trickery, deception");
        player.sendMessage("");
        player.sendMessage("§f§lNo Mana §7— runs on the Noise Meter (Resonance) instead");
        player.sendMessage("§7 - Speed II always, Speed III once Resonance rises above 0%");
        player.sendMessage("§7 - Immune to Warden targeting and Sonic Boom damage");
        player.sendMessage("§7 - Resonance rises from combat and nearby explosions, drains when out of combat");
        player.sendMessage("§7 - §f0-25% Echo§7: extra Resonance on hit, fake hit sounds");
        player.sendMessage("§7 - §f25-50% Distorted Echo§7: slight flicker, quiet footsteps, hidden nametag");
        player.sendMessage("§7 - §f50-75% Lost Signal§7: heavier flicker, illusion footsteps + fake swings");
        player.sendMessage("§7 - §f75-100% Perfect Echo§7: fully invisible, afterimages, briefly revealed on attack");
        player.sendMessage("");
        player.sendMessage("§7§oAbilities (False Reflection, Vanishing Strike, Absolute Echo) are not implemented yet.");
        player.sendMessage("§8§m----------------------------------");
    }

    private void showVoidDetail(Player player) {
        player.sendMessage("§8§m----------------------------------");
        player.sendMessage("§5§l✦ Void Soul §7— §7Space, emptiness, decay");
        player.sendMessage("");
        player.sendMessage("§f§lPassive:");
        player.sendMessage("§7 - Resistance below 30% health");
        player.sendMessage("§7 - Drains nearby enemy mana while you're at full HP");
        player.sendMessage("");
        player.sendMessage("§f§lAbility 1 §7(/soul ability1) - Void Step:");
        player.sendMessage("§7 Arms a short dash, use again to teleport + slash everything in the path");
        player.sendMessage("");
        player.sendMessage("§f§lAbility 2 §7(/soul ability2) - Abyss Mark:");
        player.sendMessage("§7 Costs all your mana - marks a target for combo bonuses");
        player.sendMessage("");
        player.sendMessage("§f§lNull Field §7(Sneak + F, Ultimate):");
        player.sendMessage("§7 Instant burst - weakens caught enemies for 45s");
        player.sendMessage("§8§m----------------------------------");
    }

    private void showBloodDetail(Player player) {
        player.sendMessage("§8§m----------------------------------");
        player.sendMessage("§4§l✦ Blood Soul §7— §7Sacrifice, control, health");
        player.sendMessage("");
        player.sendMessage("§f§lPassives:");
        player.sendMessage("§7 - Blood Pact: below 40% HP, gain speed, attack speed, lifesteal");
        player.sendMessage("§7 - Blood Sense: detect low-HP enemies within 10 blocks");
        player.sendMessage("");
        player.sendMessage("§f§lAbility 1 §7(/soul ability1) - Curse:");
        player.sendMessage("§7 3s channel, then locks target - choose a debuff");
        player.sendMessage("");
        player.sendMessage("§f§lAbility 2 §7(/soul ability2) - Hemorrhage:");
        player.sendMessage("§7 Mark that builds stacks - ruptures at 5 hits for 5 hearts true damage");
        player.sendMessage("");
        player.sendMessage("§f§lBlood Hands / Puncture §7(Sneak + F, Ultimate):");
        player.sendMessage("§7 Above 5 hearts: Blood Hands (costs HP). At/below 5 hearts: Puncture (free)");
        player.sendMessage("§8§m----------------------------------");
    }

    private String formatSoulName(SoulType type) {
        String name = type.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " Soul";
    }
}