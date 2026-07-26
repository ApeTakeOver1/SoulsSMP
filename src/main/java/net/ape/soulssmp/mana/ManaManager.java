package net.ape.soulssmp.mana;

import net.ape.soulssmp.player.PlayerData;
import net.ape.soulssmp.player.PlayerDataManager;
import org.bukkit.entity.Player;

public class ManaManager {

    private final PlayerDataManager playerDataManager;

    public ManaManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    /**
     * Gets the player's current mana.
     */
    public int getMana(Player player) {
        return playerDataManager.getPlayerData(player).getMana();
    }

    /**
     * Gets the player's max mana.
     */
    public int getMaxMana(Player player) {
        return playerDataManager.getPlayerData(player).getMaxMana();
    }

    /**
     * Sets the player's current mana, clamped between 0 and max mana.
     */
    public void setMana(Player player, int amount) {
        PlayerData data = playerDataManager.getPlayerData(player);
        int clamped = Math.max(0, Math.min(amount, data.getMaxMana()));
        data.setMana(clamped);
    }

    /**
     * Sets the player's max mana. Does NOT change current mana,
     * unless current mana now exceeds the new max (then it gets clamped down).
     */
    public void setMaxMana(Player player, int amount) {
        PlayerData data = playerDataManager.getPlayerData(player);
        data.setMaxMana(amount);

        if (data.getMana() > amount) {
            data.setMana(amount);
        }
    }

    /**
     * Adds mana, clamped so it can't go above max mana.
     */
    public void addMana(Player player, int amount) {
        setMana(player, getMana(player) + amount);
    }

    /**
     * Checks if the player has enough mana to use something.
     */
    public boolean hasEnoughMana(Player player, int cost) {
        return getMana(player) >= cost;
    }

    /**
     * Attempts to spend mana. Returns true if successful (had enough),
     * false if the player didn't have enough mana (nothing is deducted).
     */
    public boolean spendMana(Player player, int cost) {
        if (!hasEnoughMana(player, cost)) {
            return false;
        }

        setMana(player, getMana(player) - cost);
        return true;
    }

    /**
     * Fully refills mana to max.
     */
    public void refillMana(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        data.setMana(data.getMana());
        setMana(player, data.getMaxMana());
    }
}