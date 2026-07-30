package net.ape.soulssmp.listeners;

import net.ape.soulssmp.SoulsSMP;
import net.ape.soulssmp.api.AbilityType;
import net.ape.soulssmp.data.PlayerData;
import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.abilities.voidsoul.active.AbyssMark;
import net.ape.soulssmp.abilities.voidsoul.ultimate.NullField;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class VoidCombatListener implements Listener {

    private static final double NULL_FIELD_DAMAGE_MULTIPLIER = 1.5;
    private static final int MANA_ON_NORMAL_ATTACK_AMOUNT = 5; // Mana gained per normal hit on another player
    private static final int MANA_ON_CRIT_ATTACK_AMOUNT = 10; // Mana gained per critical hit on another player

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        PlayerData attackerData = SoulsSMP.getInstance().getPlayerDataManager().getPlayerData(attacker);
        if (attackerData.getSoul() != SoulType.VOID) return;

        // Anyone affected by Null Field takes 1.5x more damage from the caster, on every hit
        if (SoulsSMP.getInstance().getAbilityManager().getAs(AbilityType.NULL_FIELD, NullField.class)
                .isAffected(attacker.getUniqueId(), target.getUniqueId())) {
            event.setDamage(event.getDamage() * NULL_FIELD_DAMAGE_MULTIPLIER);
        }

        // Abyss Mark: just build the stack, nothing else happens - it caps at 5 and stops.
        handleAbyssMarkStack(attacker, target);

        // Handle Mana on Attack Passive (only if target is a Player)
        if (target instanceof Player) {
            handleManaOnAttackPassive(attacker, event);
        }
    }

    private void handleAbyssMarkStack(Player attacker, LivingEntity target) {
        AbyssMark abyssMark = SoulsSMP.getInstance().getAbilityManager().getAs(AbilityType.ABYSS_MARK, AbyssMark.class);
        if (!abyssMark.isMarkedBy(target.getUniqueId(), attacker.getUniqueId())) return;

        int stacks = abyssMark.registerHit(target.getUniqueId(), attacker.getUniqueId());
        if (stacks <= 0) return;

        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 6 + stacks * 2, 0.2, 0.3, 0.2, 0.01);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.0f + (stacks * 0.1f));
    }

    private void handleManaOnAttackPassive(Player attacker, EntityDamageByEntityEvent event) {
        boolean isCritical = event.isCritical(); // Use the isCritical() method from EntityDamageEvent

        int manaToGain = isCritical ? MANA_ON_CRIT_ATTACK_AMOUNT : MANA_ON_NORMAL_ATTACK_AMOUNT;

        SoulsSMP.getInstance().getManaManager().addMana(attacker, manaToGain);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }
}