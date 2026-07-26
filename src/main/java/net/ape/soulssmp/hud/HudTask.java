package net.ape.soulssmp.hud;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.Ability;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.player.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Repeating task that keeps every player's HUD (mana bar, ultimate bar,
 * action bar text for Void Step / Abyss Mark) updated in real time.
 */
public class HudTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateManaBar(player);
            updateUltimateBar(player);
            updateActionBar(player);
        }
    }

    private void updateManaBar(Player player) {
        BossBar manaBar = SoulsSMP.getInstance().getHudManager().getManaBar(player);
        if (manaBar == null) return;

        PlayerData data = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player);
        int mana = data.getMana();
        int maxMana = data.getMaxMana();

        double progress = maxMana <= 0 ? 0 : Math.max(0, Math.min(1.0, (double) mana / maxMana));
        manaBar.setProgress(progress);
        manaBar.setTitle("§d§lMana §7» §f" + mana + " §7/ §f" + maxMana);
    }

    private void updateUltimateBar(Player player) {
        BossBar ultimateBar = SoulsSMP.getInstance().getHudManager().getUltimateBar(player);
        if (ultimateBar == null) return;

        Ability nullField = SoulsSMP.getInstance().getAbilityManager().getAbility(AbilityType.NULL_FIELD);
        if (nullField == null) return;

        long remaining = SoulsSMP.getInstance().getAbilityManager()
                .getRemainingCooldownSeconds(player, AbilityType.NULL_FIELD);
        int totalCooldown = nullField.getCooldownSeconds();

        if (remaining <= 0) {
            ultimateBar.setProgress(1.0);
            ultimateBar.setTitle("§f§lNull Field §7» §aREADY §7(Sneak + F)");
        } else {
            double progress = 1.0 - ((double) remaining / totalCooldown);
            ultimateBar.setProgress(Math.max(0, Math.min(1.0, progress)));
            ultimateBar.setTitle("§f§lNull Field §7» §c" + remaining + "s");
        }
    }

    private void updateActionBar(Player player) {
        String voidStepText = buildAbilityBar(player, AbilityType.VOID_STEP, "Void Step");
        String abyssMarkText = buildAbilityBar(player, AbilityType.ABYSS_MARK, "Abyss Mark");

        String fullText = voidStepText + "   §8|   " + abyssMarkText;

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(fullText));
    }

    private String buildAbilityBar(Player player, AbilityType type, String label) {
        Ability ability = SoulsSMP.getInstance().getAbilityManager().getAbility(type);
        if (ability == null) return "§7" + label + ": §8N/A";

        long remaining = SoulsSMP.getInstance().getAbilityManager()
                .getRemainingCooldownSeconds(player, type);
        int totalCooldown = ability.getCooldownSeconds();

        if (remaining <= 0) {
            return "§5§l" + label + " §7» §aREADY";
        }

        int totalSegments = 10;
        int filled = (int) Math.round(((double) (totalCooldown - remaining) / totalCooldown) * totalSegments);
        filled = Math.max(0, Math.min(totalSegments, filled));

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < totalSegments; i++) {
            bar.append(i < filled ? "§5▌" : "§8▌");
        }

        return "§5§l" + label + " §7» " + bar + " §7" + remaining + "s";
    }
}