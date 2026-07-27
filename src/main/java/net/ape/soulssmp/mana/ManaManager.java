package net.ape.soulssmp.mana;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.player.PlayerData;
import net.ape.soulssmp.player.PlayerDataManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class ManaManager {

    private final PlayerDataManager playerDataManager;

    public ManaManager(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    public int getMana(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return getMaxMana(player);
        }
        return playerDataManager.getPlayerData(player).getMana();
    }

    public int getMaxMana(Player player) {
        int max = playerDataManager.getPlayerData(player).getMaxMana();
        return max <= 0 ? 100 : max;
    }

    public void setMana(Player player, int amount) {
        PlayerData data = playerDataManager.getPlayerData(player);
        int clamped = Math.max(0, Math.min(amount, data.getMaxMana()));
        data.setMana(clamped);
    }

    public void setMaxMana(Player player, int amount) {
        PlayerData data = playerDataManager.getPlayerData(player);
        data.setMaxMana(amount);

        if (data.getMana() > amount) {
            data.setMana(amount);
        }
    }

    public void addMana(Player player, int amount) {
        setMana(player, getMana(player) + amount);
    }

    public boolean hasEnoughMana(Player player, int cost) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        return getMana(player) >= getActualCost(player, cost);
    }

    /**
     * Attempts to spend mana. Applies Silence's cost multiplier if active.
     * In Creative mode, always succeeds and nothing is deducted.
     */
    public boolean spendMana(Player player, int cost) {
        if (player.getGameMode() == GameMode.CREATIVE) return true;

        int actualCost = getActualCost(player, cost);

        if (getMana(player) < actualCost) {
            return false;
        }

        setMana(player, getMana(player) - actualCost);
        return true;
    }

    private int getActualCost(Player player, int baseCost) {
        if (SoulsSMP.getInstance().getSilenceManager().isSilenced(player)) {
            return (int) Math.ceil(baseCost * SoulsSMP.getInstance().getSilenceManager().getCostMultiplier());
        }
        return baseCost;
    }

    public void refillMana(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        setMana(player, data.getMaxMana());
    }
}