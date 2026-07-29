package net.ape.soulssmp.commands;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.api.soul.SoulDefinition;
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

        switch (args[0].toLowerCase()) {
            case "ability1" -> useMappedAbility(player, 1);
            case "ability2" -> useMappedAbility(player, 2);
            case "info" -> handleInfo(player, args);
            case "set" -> handleSet(player, args);
            case "mana" -> handleMana(player, args);
            default -> player.sendMessage("§8§lSoul §7» §cUnknown subcommand. Try §f/soul§7, §f/soul info§7, " +
                    "§f/soul ability1§7, §f/soul ability2§7, §f/soul set§7, or §f/soul mana");
        }

        return true;
    }

    private void useMappedAbility(Player player, int slot) {
        PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);

        if (data.getSoul() == null) {
            player.sendMessage("§8§lSoul §7» §cYou have no Soul bound.");
            return;
        }

        SoulDefinition def = SoulsSMP.getInstance().getSoulRegistry().get(data.getSoul());
        AbilityType type = slot == 1 ? def.getAbility1() : def.getAbility2();

        if (type == null) {
            player.sendMessage("§8§lSoul §7» §cYour Soul has no ability in that slot.");
            return;
        }

        SoulsSMP.getInstance().getAbilityManager().useAbility(player, type);
    }

    private void handleSet(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage("§8§lSoul §7» §cYou don't have permission to do that.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§8§lSoul §7» §7Usage: /soul set <void|blood|echo>");
            return;
        }

        SoulType type;
        try {
            type = SoulType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§8§lSoul §7» §cUnknown soul: " + args[1]);
            return;
        }

        SoulsSMP.getInstance().getSoulManager().setSoul(player, type);

        if (type == SoulType.ECHO) {
            SoulsSMP.getInstance().getResonanceManager().setResonance(player, 0);
            player.sendMessage("§8§lSoul §7» §fYou are now bound to the Echo Soul. Resonance reset to 0.");
        } else {
            SoulsSMP.getInstance().getManaManager().setMaxMana(player, 100);
            SoulsSMP.getInstance().getManaManager().setMana(player, 100);
            player.sendMessage("§8§lSoul §7» §fYou are now bound to the " + formatSoulName(type) + ". Mana set to 100/100.");
        }
    }

    private void handleMana(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage("§8§lSoul §7» §cYou don't have permission to do that.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§8§lSoul §7» §7Usage: /soul mana <amount>");
            return;
        }

        try {
            int amount = Integer.parseInt(args[1]);
            SoulsSMP.getInstance().getManaManager().setMana(player, amount);
            player.sendMessage("§8§lSoul §7» §fMana set to " + amount + ".");
        } catch (NumberFormatException e) {
            player.sendMessage("§8§lSoul §7» §cThat's not a number.");
        }
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
            String status = hasImplementedAbilities(type) ? "§a(available)" : "§e(passive live, abilities in progress)";
            player.sendMessage("§7 - §f" + formatSoulName(type) + " " + status);
        }

        player.sendMessage("§8§m----------------------------------");
    }

    private boolean hasImplementedAbilities(SoulType type) {
        SoulDefinition def = SoulsSMP.getInstance().getSoulRegistry().get(type);
        if (def == null) return false;

        var abilityManager = SoulsSMP.getInstance().getAbilityManager();
        return (def.getAbility1() != null && abilityManager.getAbility(def.getAbility1()) != null)
                || (def.getUltimate() != null && abilityManager.getAbility(def.getUltimate()) != null);
    }

    private void showSoulDetail(Player player, String soulArg) {
        SoulType type;
        try {
            type = SoulType.valueOf(soulArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§8§lSoul §7» §cUnknown soul: " + soulArg);
            return;
        }

        SoulDefinition def = SoulsSMP.getInstance().getSoulRegistry().get(type);
        if (def == null) {
            player.sendMessage("§8§lSoul §7» §cUnknown soul: " + soulArg);
            return;
        }

        player.sendMessage("§8§m----------------------------------");
        def.getInfoLines().forEach(player::sendMessage);
        player.sendMessage("§8§m----------------------------------");
    }

    private String formatSoulName(SoulType type) {
        String name = type.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " Soul";
    }
}
