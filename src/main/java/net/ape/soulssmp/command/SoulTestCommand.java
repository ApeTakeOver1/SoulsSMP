package net.ape.soulssmp.command;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * TEMPORARY testing command. Not part of real progression —
 * lets you force a soul + mana onto yourself so you can test abilities
 * before the real soul-assignment system (rituals, etc.) exists.
 */
public class SoulTestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§8§lSoulTest §7» §7Usage: /soultest void | /soultest mana <amount>");
            return true;
        }

        if (args[0].equalsIgnoreCase("void")) {
            SoulsSMP.getInstance().getSoulManager().setSoul(player, SoulType.VOID);
            SoulsSMP.getInstance().getManaManager().setMaxMana(player, 100);
            SoulsSMP.getInstance().getManaManager().setMana(player, 100);
            player.sendMessage("§8§lSoulTest §7» §5You are now bound to the Void Soul. Mana set to 100/100.");
            return true;
        }

        if (args[0].equalsIgnoreCase("mana") && args.length > 1) {
            try {
                int amount = Integer.parseInt(args[1]);
                SoulsSMP.getInstance().getManaManager().setMana(player, amount);
                player.sendMessage("§8§lSoulTest §7» §5Mana set to " + amount + ".");
            } catch (NumberFormatException e) {
                player.sendMessage("§8§lSoulTest §7» §cThat's not a number.");
            }
            return true;
        }

        player.sendMessage("§8§lSoulTest §7» §cUnknown usage.");
        return true;
    }
}