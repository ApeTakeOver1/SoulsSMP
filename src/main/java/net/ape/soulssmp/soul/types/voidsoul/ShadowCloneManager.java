package net.ape.soulssmp.soul.types.voidsoul;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.EulerAngle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the black "shadow clone" ArmorStand used as Void Step's afterimage.
 * Only one clone per player at a time.
 */
public class ShadowCloneManager {

    private final Map<UUID, ArmorStand> activeClones = new HashMap<>();

    public void spawnClone(Player player, Location location) {
        removeClone(player);

        ArmorStand clone = location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setInvisible(false);
            stand.setBasePlate(false);
            stand.setArms(true);
            stand.setGravity(false);
            stand.setCustomNameVisible(false);
            stand.setInvulnerable(true);
            stand.setSmall(false);
            stand.setMarker(false);
            stand.setCollidable(false);

            stand.getEquipment().setHelmet(dyedBlack(Material.LEATHER_HELMET));
            stand.getEquipment().setChestplate(dyedBlack(Material.LEATHER_CHESTPLATE));
            stand.getEquipment().setLeggings(dyedBlack(Material.LEATHER_LEGGINGS));
            stand.getEquipment().setBoots(dyedBlack(Material.LEATHER_BOOTS));

            // Dash-ready stance: leaned forward, arm drawn back
            stand.setRightArmPose(new EulerAngle(Math.toRadians(-70), 0, 0));
            stand.setLeftArmPose(new EulerAngle(Math.toRadians(25), 0, 0));
            stand.setHeadPose(new EulerAngle(Math.toRadians(10), 0, 0));

            stand.setRotation(location.getYaw(), 0);
        });

        activeClones.put(player.getUniqueId(), clone);
    }

    public void removeClone(Player player) {
        ArmorStand clone = activeClones.remove(player.getUniqueId());
        if (clone != null && !clone.isDead()) {
            clone.remove();
        }
    }

    public boolean hasClone(Player player) {
        ArmorStand clone = activeClones.get(player.getUniqueId());
        return clone != null && !clone.isDead();
    }

    /**
     * Removes every active clone. Call this on plugin disable so a
     * /reload or shutdown doesn't leave floating armor stands behind.
     */
    public void removeAll() {
        for (ArmorStand stand : activeClones.values()) {
            if (!stand.isDead()) {
                stand.remove();
            }
        }
        activeClones.clear();
    }

    private ItemStack dyedBlack(Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.BLACK);
        item.setItemMeta(meta);
        return item;
    }
}