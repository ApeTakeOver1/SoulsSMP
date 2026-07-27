package net.ape.soulssmp.player;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.soul.SoulType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new PlayerData()
        );
    }

    public void removePlayerData(Player player) {
        playerDataMap.remove(player.getUniqueId());
    }

    public boolean hasPlayerData(Player player) {
        return playerDataMap.containsKey(player.getUniqueId());
    }

    /**
     * Loads a player's data from disk (playerdata/<uuid>.yml) into memory.
     * If no file exists yet (first ever join), creates a fresh blank PlayerData.
     * Call this on PlayerJoinEvent.
     */
    public void loadPlayerData(Player player) {
        File file = getPlayerFile(player.getUniqueId());
        PlayerData data = new PlayerData();

        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            String soulName = config.getString("soul");
            if (soulName != null) {
                try {
                    data.setSoul(SoulType.valueOf(soulName));
                } catch (IllegalArgumentException ignored) {
                    // saved soul name no longer exists, leave as null
                }
            }

            data.setMana(config.getInt("mana", 0));
            data.setMaxMana(config.getInt("maxMana", 0));
            data.setFavor(config.getInt("favor", 0));
            data.setResurrectionCount(config.getInt("resurrectionCount", 0));
            data.setAwakened(config.getBoolean("awakened", false));
        }

        playerDataMap.put(player.getUniqueId(), data);
    }

    /**
     * Saves a player's current in-memory data to disk.
     * Call this on PlayerQuitEvent, and again on plugin disable for safety.
     */
    public void savePlayerData(Player player) {
        if (!hasPlayerData(player)) return;

        PlayerData data = playerDataMap.get(player.getUniqueId());
        File file = getPlayerFile(player.getUniqueId());
        YamlConfiguration config = new YamlConfiguration();

        config.set("soul", data.getSoul() == null ? null : data.getSoul().name());
        config.set("mana", data.getMana());
        config.set("maxMana", data.getMaxMana());
        config.set("favor", data.getFavor());
        config.set("resurrectionCount", data.getResurrectionCount());
        config.set("awakened", data.isAwakened());

        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (Exception e) {
            SoulsSMP.getInstance().getLogger().warning(
                    "Failed to save player data for " + player.getName() + ": " + e.getMessage()
            );
        }
    }

    /**
     * Saves every currently-loaded player's data. Call on plugin disable
     * so a server stop/restart doesn't lose anyone's progress.
     */
    public void saveAll() {
        for (UUID uuid : playerDataMap.keySet()) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) {
                savePlayerData(player);
            }
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(SoulsSMP.getInstance().getDataFolder(), "playerdata/" + uuid + ".yml");
    }
}