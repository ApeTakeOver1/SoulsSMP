package net.ape.soulssmp.player;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    /**
     * Gets a player's data.
     * Creates it if it doesn't exist.
     */
    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new PlayerData()
        );
    }

    /**
     * Removes a player's data from memory.
     * (We'll save it before removing later.)
     */
    public void removePlayerData(Player player) {
        playerDataMap.remove(player.getUniqueId());
    }

    /**
     * Checks if a player's data is loaded.
     */
    public boolean hasPlayerData(Player player) {
        return playerDataMap.containsKey(player.getUniqueId());
    }

}