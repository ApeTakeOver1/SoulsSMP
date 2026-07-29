package net.ape.soulssmp.listeners;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.data.PlayerData;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.abilities.voidsoul.active.AbyssMark;
import net.ape.soulssmp.abilities.voidsoul.ultimate.NullField;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class VoidCombatListener implements Listener {

    private static final String VOID_PREFIX = ChatColor.DARK_GRAY.toString() + ChatColor.BOLD + "Void " + ChatColor.GRAY + "» " + ChatColor.DARK_PURPLE;
    private static final double ABYSS_MARK_BONUS_DAMAGE_MULTIPLIER = 1.25;
    private static final double NULL_FIELD_DAMAGE_MULTIPLIER = 1.5;
    private static final int ABYSS_MARK_MANA_REFUND = 10;
    private static final int ABYSS_MARK_NULL_FIELD_MANA_REFUND = 20;
    private static final int ABYSS_MARK_NULL_FIELD_COOLDOWN_REDUCTION = 5;
    private static final int ABYSS_MARK_NULL_FIELD_COOLDOWN_REDUCTION_BOOSTED = 10;
    private static final int ABYSS_MARK_STUN_DURATION_TICKS = 14; // 0.7 seconds
    private static final int MANA_ON_NORMAL_ATTACK_AMOUNT = 5; // Mana gained per normal hit on another player
    private static final int MANA_ON_CRIT_ATTACK_AMOUNT = 10; // Mana gained per critical hit on another player
    private static final long ACTION_BAR_OVERRIDE_MILLIS = 1500L;


    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        PlayerData attackerData = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(attacker);
        if (attackerData.getSoul() != SoulType.VOID) return;

        // Anyone affected by Null Field takes 1.5x more damage from the caster, on every hit
        if (SoulsSMP.getInstance().getNullField().isAffected(attacker.getUniqueId(), target.getUniqueId())) {
            event.setDamage(event.getDamage() * NULL_FIELD_DAMAGE_MULTIPLIER);
        }

        // Handle Abyss Mark combo
        handleAbyssMarkCombo(attacker, target, event);

        // Handle Mana on Attack Passive (only if target is a Player)
        if (target instanceof Player) {
            handleManaOnAttackPassive(attacker, event);
        }
    }

    private void handleAbyssMarkCombo(Player attacker, LivingEntity target, EntityDamageByEntityEvent event) {
        AbyssMark abyssMark = SoulsSMP.getInstance().getAbyssMark();
        if (!abyssMark.isMarkedBy(target, attacker.getUniqueId())) return;

        int hitCount = abyssMark.registerHit(target, attacker.getUniqueId());
        if (hitCount == -1) return; // Mark expired or not marked by this attacker

        NullField nullField = SoulsSMP.getInstance().getNullField();
        boolean isNullFieldAffected = nullField.isAffected(attacker.getUniqueId(), target.getUniqueId());

        if (hitCount % 3 == 0) {
            // Rapid-fire slashes for 3rd hit combo (base damage already reflects the Null Field 1.5x if applicable)
            int numSlashes = 3; // Number of rapid slashes
            double baseDamage = event.getDamage();

            for (int i = 0; i < numSlashes; i++) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!target.isDead() && target.isValid()) {
                            target.damage(baseDamage * ABYSS_MARK_BONUS_DAMAGE_MULTIPLIER, attacker);
                            target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
                        }
                    }
                }.runTaskLater(SoulsSMP.getInstance(), i * 3L); // 3 ticks delay between slashes
            }
            SoulsSMP.getInstance().getHudManager().showActionBarOverride(attacker, VOID_PREFIX + "Combo Strike!", ACTION_BAR_OVERRIDE_MILLIS);

            // Mana refund
            int manaRefund = isNullFieldAffected ? ABYSS_MARK_NULL_FIELD_MANA_REFUND : ABYSS_MARK_MANA_REFUND;
            SoulsSMP.getInstance().getManaManager().addMana(attacker, manaRefund);
            SoulsSMP.getInstance().getHudManager().showActionBarOverride(attacker, VOID_PREFIX + "+" + manaRefund + " Mana!", ACTION_BAR_OVERRIDE_MILLIS);

            // Null Field cooldown reduction (always applies; boosted if target is Null Field affected)
            int cooldownReduction = isNullFieldAffected ? ABYSS_MARK_NULL_FIELD_COOLDOWN_REDUCTION_BOOSTED : ABYSS_MARK_NULL_FIELD_COOLDOWN_REDUCTION;
            SoulsSMP.getInstance().getAbilityManager().reduceCooldown(attacker, AbilityType.NULL_FIELD, cooldownReduction);
            SoulsSMP.getInstance().getHudManager().showActionBarOverride(attacker, VOID_PREFIX + "Null Field Cooldown Reduced!", ACTION_BAR_OVERRIDE_MILLIS);
        }

        if (hitCount % 5 == 0) {
            // Stun if Null Field affected
            if (isNullFieldAffected && target instanceof Player targetPlayer) {
                // StunManager cancels PlayerMoveEvent outright for a true full stop,
                // instead of approximating one with Slowness (which only scales speed
                // down and never actually reaches zero, and does nothing to jumping).
                SoulsSMP.getInstance().getStunManager().stun(targetPlayer, ABYSS_MARK_STUN_DURATION_TICKS);
                targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.0f, 0.8f);
                attacker.playSound(attacker.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.0f, 0.8f);
                SoulsSMP.getInstance().getHudManager().showActionBarOverride(targetPlayer,
                        ChatColor.DARK_GRAY.toString() + ChatColor.BOLD + "STUNNED " + ChatColor.GRAY + "» " + ChatColor.RED + "You were stunned by Abyss Mark!", ACTION_BAR_OVERRIDE_MILLIS);
                SoulsSMP.getInstance().getHudManager().showActionBarOverride(attacker, VOID_PREFIX + "Target Stunned!", ACTION_BAR_OVERRIDE_MILLIS);
            }
        }
    }

    private void handleManaOnAttackPassive(Player attacker, EntityDamageByEntityEvent event) {
        boolean isCritical = event.isCritical(); // Use the isCritical() method from EntityDamageEvent

        int manaToGain = isCritical ? MANA_ON_CRIT_ATTACK_AMOUNT : MANA_ON_NORMAL_ATTACK_AMOUNT;

        SoulsSMP.getInstance().getManaManager().addMana(attacker, manaToGain);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }
}