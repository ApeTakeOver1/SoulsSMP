package net.ape.soulssmp.command;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.player.PlayerData;
import net.ape.soulssmp.soul.SoulType;
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

        if (args[0].equalsIgnoreCase("ability")) {
            handleAbilityMenu(player, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            handleInfo(player, args);
            return true;
        }

        player.sendMessage("§8§lSoul §7» §cUnknown subcommand. Try §f/soul§7, §f/soul info§7, or §f/soul ability");
        return true;
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
            player.sendMessage("§7Mana: §f" + (creative ? "∞" : data.getMana() + " / " + data.getMaxMana()));
            player.sendMessage("§7Favor: §f" + data.getFavor() + " §7(-5 to +5)");
            player.sendMessage("§7Resurrections: §f" + data.getResurrectionCount());
            player.sendMessage("§7Awakened Weapon: §f" + (data.isAwakened() ? "Yes" : "No"));
        }

        if (creative) {
            player.sendMessage("§7§oCreative Mode: infinite mana, no cooldowns.");
        }

        player.sendMessage("§8§m----------------------------------");
    }

    private void handleAbilityMenu(Player player, String[] args) {
        PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);

        if (args.length == 1) {
            player.sendMessage("§8§l✦ Your Abilities §7» §fChoose one:");

            if (data.getSoul() == SoulType.VOID) {
                player.sendMessage("§7 - §5/soul ability voidstep");
                player.sendMessage("§7 - §5/soul ability abyssmark");
            } else if (data.getSoul() == SoulType.BLOOD) {
                player.sendMessage("§7 - §4/soul ability curse");
                player.sendMessage("§7 - §4/soul ability hemorrhage");
            } else {
                player.sendMessage("§7You have no Soul bound.");
            }
            return;
        }

        AbilityType type = switch (args[1].toLowerCase()) {
            case "voidstep" -> AbilityType.VOID_STEP;
            case "abyssmark" -> AbilityType.ABYSS_MARK;
            case "curse" -> AbilityType.CURSE;
            case "hemorrhage" -> AbilityType.HEMORRHAGE;
            default -> null;
        };

        if (type == null) {
            player.sendMessage("§8§lSoul §7» §cUnknown ability: " + args[1]);
            return;
        }

        SoulsSMP.getInstance().getAbilityManager().useAbility(player, type);
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
            player.sendMessage("§7 - §f" + formatSoulName(type) + " §a(available)");
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
        }
    }

    private void showVoidDetail(Player player) {
        player.sendMessage("§8§m----------------------------------");
        player.sendMessage("§5§l✦ Void Soul §7— §7Space, emptiness, decay");
        player.sendMessage("");
        player.sendMessage("§f§lPassive:");
        player.sendMessage("§7 - Resistance below 30% health");
        player.sendMessage("§7 - Drains nearby enemy mana while you're at full HP");
        player.sendMessage("");
        player.sendMessage("§f§lVoid Step §7(/soul ability voidstep):");
        player.sendMessage("§7 Arms a short dash, use again to teleport + slash everything in the path");
        player.sendMessage("");
        player.sendMessage("§f§lAbyss Mark §7(/soul ability abyssmark):");
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
        player.sendMessage("§f§lCurse §7(/soul ability curse):");
        player.sendMessage("§7 3s channel, then locks target with a random debuff");
        player.sendMessage("");
        player.sendMessage("§f§lHemorrhage §7(/soul ability hemorrhage):");
        player.sendMessage("§7 Mark that builds stacks - ruptures at 5 hits");
        player.sendMessage("");
        player.sendMessage("§f§lBlood Hands §7(Sneak + F, Ultimate):");
        player.sendMessage("§7 Costs HP, not mana - random powerful effect");
        player.sendMessage("§8§m----------------------------------");
    }

    private String formatSoulName(SoulType type) {
        String name = type.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " Soul";
    }
}