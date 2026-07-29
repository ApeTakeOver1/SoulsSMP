package net.ape.soulssmp.tasks;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.Ability;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.abilities.blood.ultimate.BloodHands;
import net.ape.soulssmp.abilities.voidsoul.active.AbyssMark;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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

        if (player.getGameMode() == GameMode.CREATIVE) {
            manaBar.setProgress(1.0);
            manaBar.setTitle("§d§lMana §7» §f∞");
            return;
        }

        int mana = SoulsSMP.getInstance().getManaManager().getMana(player);
        int maxMana = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player).getMaxMana();

        double progress = maxMana <= 0 ? 0 : Math.max(0, Math.min(1.0, (double) mana / maxMana));
        manaBar.setProgress(progress);
        manaBar.setTitle("§d§lMana §7» §f" + mana + " §7/ §f" + maxMana);
    }

    private void updateUltimateBar(Player player) {
        BossBar ultimateBar = SoulsSMP.getInstance().getHudManager().getUltimateBar(player);
        if (ultimateBar == null) return;

        SoulType soul = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player).getSoul();

        if (soul == null) {
            ultimateBar.setTitle("§7No Ultimate");
            ultimateBar.setProgress(0);
            return;
        }

        AbilityType ultimateType = soul == SoulType.BLOOD ? AbilityType.BLOOD_HANDS
                : soul == SoulType.VOID ? AbilityType.NULL_FIELD : null;

        if (ultimateType == null) {
            ultimateBar.setTitle("§7No Ultimate");
            ultimateBar.setProgress(0);
            return;
        }

        Ability ultimate = SoulsSMP.getInstance().getAbilityManager().getAbility(ultimateType);
        if (ultimate == null) return;

        String label;
        String hint = " (Sneak + F)";

        if (ultimateType == AbilityType.BLOOD_HANDS) {
            boolean punctureMode = player.getHealth() <= BloodHands.PUNCTURE_HP_THRESHOLD;
            label = punctureMode ? "§c§lPuncture" : "§4§lBlood Hands";
        } else {
            label = "§f§lNull Field";
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            ultimateBar.setProgress(1.0);
            ultimateBar.setTitle(label + " §7» §aREADY §7" + hint);
            return;
        }

        long remaining = SoulsSMP.getInstance().getAbilityManager()
                .getRemainingCooldownSeconds(player, ultimateType);
        int totalCooldown = ultimate.getCooldownSeconds();

        if (remaining <= 0) {
            ultimateBar.setProgress(1.0);
            ultimateBar.setTitle(label + " §7» §aREADY §7" + hint);
        } else {
            double progress = 1.0 - ((double) remaining / totalCooldown);
            ultimateBar.setProgress(Math.max(0, Math.min(1.0, progress)));
            ultimateBar.setTitle(label + " §7» §c" + remaining + "s");
        }
    }

    private void updateActionBar(Player player) {
        String override = SoulsSMP.getInstance().getHudManager().getActionBarOverride(player.getUniqueId());
        if (override != null) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(override));
            return;
        }

        SoulType soul = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(player).getSoul();

        String fullText;
        if (soul == SoulType.VOID) {
            String voidStepText = buildCooldownBar(player, AbilityType.VOID_STEP, "Void Step", "§d");
            String abyssMarkText = buildAbyssMarkBar(player);
            fullText = voidStepText + "   §8|   " + abyssMarkText;
        } else if (soul == SoulType.BLOOD) {
            String curseText = buildCooldownBar(player, AbilityType.CURSE, "Curse", "§4");
            String hemorrhageText = buildHemorrhageBar(player);
            fullText = curseText + "   §8|   " + hemorrhageText;
        } else {
            fullText = "§7No Soul bound";
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(fullText));
    }

    private int getDisplayCost(Player player, int baseCost) {
        if (SoulsSMP.getInstance().getSilenceManager().isSilenced(player)) {
            return (int) Math.ceil(baseCost * SoulsSMP.getInstance().getSilenceManager().getCostMultiplier());
        }
        return baseCost;
    }

    private String buildCooldownBar(Player player, AbilityType type, String label, String colorCode) {
        Ability ability = SoulsSMP.getInstance().getAbilityManager().getAbility(type);
        if (ability == null) return "§7" + label + ": §8N/A";

        int displayCost = getDisplayCost(player, ability.getManaCost());
        String costPrefix = "§7(§f" + displayCost + "§7) ";

        if (player.getGameMode() == GameMode.CREATIVE) {
            return costPrefix + colorCode + "§l" + label + " §7» §aREADY";
        }

        long remaining = SoulsSMP.getInstance().getAbilityManager()
                .getRemainingCooldownSeconds(player, type);
        int totalCooldown = ability.getCooldownSeconds();

        if (remaining <= 0) {
            return costPrefix + colorCode + "§l" + label + " §7» §aREADY";
        }

        int totalSegments = 10;
        int filled = (int) Math.round(((double) (totalCooldown - remaining) / totalCooldown) * totalSegments);
        filled = Math.max(0, Math.min(totalSegments, filled));

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < totalSegments; i++) {
            bar.append(i < filled ? colorCode + "▌" : "§8▌");
        }

        return costPrefix + colorCode + "§l" + label + " §7» " + bar + " §7" + remaining + "s";
    }

    private String buildAbyssMarkBar(Player player) {
        String costPrefix = "§7(§fALL§7) "; // Abyss Mark costs all current mana

        if (player.getGameMode() == GameMode.CREATIVE) {
            return costPrefix + "§5§lAbyss Mark §7» §aREADY";
        }

        AbyssMark abyssMark = SoulsSMP.getInstance().getAbyssMark();
        int hitCount = abyssMark.getHitCount(player.getUniqueId());
        int remainingMarkSeconds = abyssMark.getRemainingMarkSeconds(player.getUniqueId());

        if (remainingMarkSeconds <= 0) {
            return costPrefix + "§5§lAbyss Mark §7» §aREADY";
        }

        StringBuilder pips = new StringBuilder();
        for (int i = 1; i <= 5; i++) { // Max 5 hits for combo
            pips.append(i <= hitCount ? "§d●" : "§8●");
        }

        return costPrefix + "§5§lAbyss Mark §7» " + pips + " §7" + hitCount + "/5 §7(" + remainingMarkSeconds + "s)";
    }

    private String buildHemorrhageBar(Player player) {
        int[] status = SoulsSMP.getInstance().getHemorrhage().getCurrentMarkStatus(player.getUniqueId());

        if (status == null) {
            return buildCooldownBar(player, AbilityType.HEMORRHAGE, "Hemorrhage", "§4");
        }

        int stacks = status[0];
        int secondsRemaining = status[1];

        StringBuilder pips = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            pips.append(i <= stacks ? "§c●" : "§8●");
        }

        String label = ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Hemorrhage";

        return label + " §7» " + pips + " §7" + stacks + "/5 §7(" + secondsRemaining + "s)";
    }
}