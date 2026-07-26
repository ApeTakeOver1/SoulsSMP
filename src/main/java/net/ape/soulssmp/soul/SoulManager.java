package net.ape.soulssmp.soul;

import net.ape.soulssmp.player.PlayerData;
import net.ape.soulssmp.player.PlayerDataManager;
import org.bukkit.entity.Player;

public class SoulManager {

    private final PlayerDataManager playerDataManager;

    public SoulManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    public SoulType getSoul(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        return data.getSoul();
    }

    public void setSoul(Player player, SoulType soulType) {
        PlayerData data = playerDataManager.getPlayerData(player);
        data.setSoul(soulType);
    }

    public boolean hasSoul(Player player) {
        return getSoul(player) != null;
    }

    public void clearSoul(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        data.setSoul(null);
    }

}