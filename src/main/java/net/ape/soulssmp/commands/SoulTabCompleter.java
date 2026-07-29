package net.ape.soulssmp.commands;

import net.ape.soulssmp.api.SoulType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class SoulTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();

        if (args.length == 1) {
            options.add("ability1");
            options.add("ability2");
            options.add("info");

            if (sender.isOp()) {
                options.add("set");
                options.add("mana");
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("set"))) {
            for (SoulType type : SoulType.values()) {
                options.add(type.name().toLowerCase());
            }
        }

        return filterMatches(options, args.length == 0 ? "" : args[args.length - 1]);
    }

    private List<String> filterMatches(List<String> options, String current) {
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(current.toLowerCase())) {
                matches.add(option);
            }
        }
        return matches;
    }
}
