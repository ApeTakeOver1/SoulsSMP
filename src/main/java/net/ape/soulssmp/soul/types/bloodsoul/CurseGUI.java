package net.ape.soulssmp.soul.types.bloodsoul;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CurseGUI {

    public static final String TITLE = "§4§lChoose Curse Effect";

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, TITLE);

        gui.setItem(2, buildItem(Material.GRAY_WOOL, "§7§lRestriction",
                List.of("§7No sprint, slowed,", "§7reduced jump height", "§8(20 seconds)")));

        gui.setItem(4, buildItem(Material.PURPLE_WOOL, "§5§lSilence",
                List.of("§7Abilities cost more,", "§7mana regen crippled,", "§7slower activation", "§8(30 seconds)")));

        gui.setItem(6, buildItem(Material.RED_WOOL, "§4§lPuppet",
                List.of("§7Leashed to you, dragged in,", "§7hotbar randomized", "§8(20 seconds)")));

        player.openInventory(gui);
    }

    private static ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}