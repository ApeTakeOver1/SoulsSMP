package net.ape.soulssmp.listener;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.ability.AbilityType;
import net.ape.soulssmp.player.PlayerData;
import net.ape.soulssmp.soul.SoulType;
import net.ape.soulssmp.soul.types.voidsoul.VoidMarkManager;
import net.ape.soulssmp.soul.types.voidsoul.NullFieldManager;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class VoidCombatListener implements Listener {

    private static final String VOID_PREFIX = ChatColor.DARK_GRAY.toString() + ChatColor.BOLD + "Void " + ChatColor.GRAY + "» " + ChatColor.DARK_PURPLE;
    private static final double ABYSS_MARK_BONUS_DAMAGE_MULTIPLIER = 1.25;
    private static final double ABYSS_MARK_NULL_FIELD_BONUS_DAMAGE_MULTIPLIER = 1.5;
    private static final int ABYSS_MARK_MANA_REFUND = 10;
    private static final int ABYSS_MARK_NULL_FIELD_MANA_REFUND = 20;
    private static final int ABYSS_MARK_NULL_FIELD_COOLDOWN_REDUCTION = 5;
    private static final int ABYSS_MARK_NULL_FIELD_COOLDOWN_REDUCTION_BOOSTED = 10;
    private static final int ABYSS_MARK_STUN_DURATION_TICKS = 40; // 2 seconds
    private static final int MANA_ON_NORMAL_ATTACK_AMOUNT = 5; // Mana gained per normal hit on another player
    private static final int MANA_ON_CRIT_ATTACK_AMOUNT = 10; // Mana gained per critical hit on another player


    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        PlayerData attackerData = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(attacker);
        if (attackerData.getSoul() != SoulType.VOID) return;

        // Handle Abyss Mark combo
        handleAbyssMarkCombo(attacker, target, event);

        // Handle Mana on Attack Passive (only if target is a Player)
        if (target instanceof Player) {
            handleManaOnAttackPassive(attacker, event);
        }
    }

    private void handleAbyssMarkCombo(Player attacker, LivingEntity target, EntityDamageByEntityEvent event) {
        VoidMarkManager voidMarkManager = SoulsSMP.getInstance().getVoidMarkManager();
        if (!voidMarkManager.isMarkedBy(target, attacker.getUniqueId())) return;

        int hitCount = voidMarkManager.registerHit(target, attacker.getUniqueId());
        if (hitCount == -1) return; // Mark expired or not marked by this attacker

        NullFieldManager nullFieldManager = SoulsSMP.getInstance().getNullFieldManager();
        boolean isNullFieldAffected = nullFieldManager.isAffected(attacker.getUniqueId(), target.getUniqueId());

        if (hitCount % 3 == 0) {
            // Rapid-fire slashes for 3rd hit combo
            double damageMultiplier = isNullFieldAffected ? ABYSS_MARK_NULL_FIELD_BONUS_DAMAGE_MULTIPLIER : ABYSS_MARK_BONUS_DAMAGE_MULTIPLIER;
            int numSlashes = 3; // Number of rapid slashes
            double baseDamage = event.getDamage(); // Use the base damage of the hit

            for (int i = 0; i < numSlashes; i++) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!target.isDead() && target.isValid()) {
                            target.damage(baseDamage * damageMultiplier, attacker);
                            target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
                        }
                    }
                }.runTaskLater(SoulsSMP.getInstance(), i * 3L); // 3 ticks delay between slashes
            }
            attacker.sendTitle("Abyss Mark", ChatColor.LIGHT_PURPLE + "Combo Slash!", 0, 40, 10);
        }

        if (hitCount % 5 == 0) {
            // Mana refund
            int manaRefund = isNullFieldAffected ? ABYSS_MARK_NULL_FIELD_MANA_REFUND : ABYSS_MARK_MANA_REFUND;
            SoulsSMP.getInstance().getManaManager().addMana(attacker, manaRefund);
            attacker.sendTitle("Abyss Mark", ChatColor.AQUA + "+" + manaRefund + " Mana!", 0, 40, 10);

            // Null Field cooldown reduction
            if (isNullFieldAffected) {
                int cooldownReduction = isNullFieldAffected ? ABYSS_MARK_NULL_FIELD_COOLDOWN_REDUCTION_BOOSTED : ABYSS_MARK_NULL_FIELD_COOLDOWN_REDUCTION;
                SoulsSMP.getInstance().getAbilityManager().reduceCooldown(attacker, AbilityType.NULL_FIELD, cooldownReduction);
                attacker.sendTitle("Abyss Mark", ChatColor.BLUE + "Null Field Cooldown Reduced!", 0, 40, 10);

                // Stun if Null Field affected
                if (target instanceof Player targetPlayer) {
                    targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ABYSS_MARK_STUN_DURATION_TICKS, 5)); // Strong slow
                    targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, ABYSS_MARK_STUN_DURATION_TICKS, 200)); // Prevent jumping (high amplifier for jump boost makes them unable to jump)
                    targetPlayer.sendTitle("STUNNED", ChatColor.RED + "You were stunned by Abyss Mark", 0, 40, 10);
                    attacker.sendTitle("Abyss Mark", ChatColor.RED + "Target Stunned!", 0, 40, 10);
                }
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