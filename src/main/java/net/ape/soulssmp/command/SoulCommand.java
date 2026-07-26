package net.ape.soulssmp.command;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.AbilityType;
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
            player.sendMessage("§8§lSoul §7» §7Usage: /soul ability");
            return true;
        }

        if (args[0].equalsIgnoreCase("ability")) {
            handleAbilityMenu(player, args);
            return true;
        }

        player.sendMessage("§8§lSoul §7» §cUnknown subcommand.");
        return true;
    }

    private void handleAbilityMenu(Player player, String[] args) {
        if (args.length == 1) {
            player.sendMessage("§8§l✦ Void Abilities §7» §fChoose one:");
            player.sendMessage("§7 - §5/soul ability voidstep");
            player.sendMessage("§7 - §5/soul ability abyssmark");
            return;
        }

        AbilityType type = switch (args[1].toLowerCase()) {
            case "voidstep" -> AbilityType.VOID_STEP;
            case "abyssmark" -> AbilityType.ABYSS_MARK;
            default -> null;
        };

        if (type == null) {
            player.sendMessage("§8§lSoul §7» §cUnknown ability: " + args[1]);
            return;
        }

        SoulsSMP.getInstance().getAbilityManager().useAbility(player, type);
    }
}