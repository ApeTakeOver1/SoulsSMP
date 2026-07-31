package net.ape.soulssmp.abilities.echo;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import net.ape.soulssmp.SoulsSMP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Packet-level helpers for Echo Soul, via ProtocolLib: hiding worn armor
 * from OTHER clients (real invisibility hides your body but not your
 * equipped armor) and spawning a genuine fake-player NPC for Perfect
 * Echo's afterimage (a real player-shaped entity with the attacker's
 * skin, not an ArmorStand puppet).
 *
 * IMPORTANT / HONEST CAVEAT: player-spawn and entity-metadata packet
 * layouts have changed across Minecraft versions, and this was written
 * without being able to compile against the real ProtocolLib jar for
 * 1.21.11 in this environment (no network access to dmulloy2's repo
 * here). The riskiest lines are marked "VERSION-SENSITIVE" below - if
 * the build fails or the afterimage looks wrong (Steve skin, T-pose,
 * wrong position), send me the exact error/symptom and I'll fix the
 * specific field it's complaining about.
 */
public class EchoPacketService {

    private static final double VIEW_RADIUS = 48.0;
    private static final AtomicInteger FAKE_ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE - 10_000);

    private static Boolean available = null;

    private EchoPacketService() {
    }

    public static boolean isAvailable() {
        if (available == null) {
            available = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
            if (!available) {
                SoulsSMP.getInstance().getLogger().warning(
                        "ProtocolLib not found - Echo Soul's true (armor-hiding) invisibility and " +
                                "real-player afterimages are disabled. Falling back to body-only invisibility " +
                                "and no afterimage. Install ProtocolLib to enable them."
                );
            }
        }
        return available;
    }

    private static ProtocolManager manager() {
        return ProtocolLibrary.getProtocolManager();
    }

    // ---------------------------------------------------------------
    // Armor hiding (true invisibility, including worn gear)
    // ---------------------------------------------------------------

    /** Sends every nearby viewer a fake "no armor/held item" packet for this player. Purely visual - real inventory untouched. */
    public static void hideEquipment(Player subject) {
        if (!isAvailable()) return;
        sendEquipmentPacket(subject, true);
    }

    /** Sends every nearby viewer the subject's REAL current equipment, undoing hideEquipment(). */
    public static void revealEquipment(Player subject) {
        if (!isAvailable()) return;
        sendEquipmentPacket(subject, false);
    }

    private static void sendEquipmentPacket(Player subject, boolean hide) {
        EntityEquipment equipment = subject.getEquipment();
        if (equipment == null) return;

        ItemStack empty = new ItemStack(Material.AIR);

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> slots = new ArrayList<>();
        slots.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, hide ? empty : equipment.getHelmet()));
        slots.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, hide ? empty : equipment.getChestplate()));
        slots.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, hide ? empty : equipment.getLeggings()));
        slots.add(new Pair<>(EnumWrappers.ItemSlot.FEET, hide ? empty : equipment.getBoots()));
        slots.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, hide ? empty : equipment.getItemInMainHand()));
        slots.add(new Pair<>(EnumWrappers.ItemSlot.OFFHAND, hide ? empty : equipment.getItemInOffHand()));

        PacketContainer packet = manager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, subject.getEntityId());
        packet.getSlotStackPairLists().write(0, slots); // VERSION-SENSITIVE: field name for the combined equipment list

        for (Player viewer : nearbyViewers(subject)) {
            try {
                manager().sendServerPacket(viewer, packet);
            } catch (Exception e) {
                SoulsSMP.getInstance().getLogger().warning("Failed to send fake equipment packet: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // Fake-player afterimage (Perfect Echo)
    // ---------------------------------------------------------------

    private static final int LUNGE_TICKS = 5;
    private static final int LINGER_TICKS_AFTER_HIT = 4;
    private static final double SPAWN_BEHIND_DISTANCE = 1.5;

    public static void spawnAfterimage(Player attacker, LivingEntity target) {
        if (!isAvailable()) return;

        int fakeEntityId = FAKE_ENTITY_ID_COUNTER.getAndDecrement();
        UUID fakeUuid = UUID.randomUUID();

        WrappedGameProfile realProfile = WrappedGameProfile.fromPlayer(attacker);
        WrappedGameProfile fakeProfile = new WrappedGameProfile(fakeUuid, attacker.getName());
        fakeProfile.getProperties().putAll(realProfile.getProperties()); // copies the skin texture property

        org.bukkit.util.Vector towardTarget = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
        towardTarget.setY(0);
        if (towardTarget.lengthSquared() < 0.0001) {
            towardTarget = attacker.getLocation().getDirection().setY(0);
        }
        towardTarget.normalize();

        Location spawnAt = attacker.getLocation().clone().subtract(towardTarget.clone().multiply(SPAWN_BEHIND_DISTANCE));
        Location targetPoint = target.getLocation().clone();

        List<Player> viewers = nearbyViewers(attacker);

        addToTabList(fakeProfile, viewers);
        sendSpawnPacket(fakeEntityId, fakeUuid, spawnAt, viewers);
        sendSkinLayerMetadata(fakeEntityId, viewers);
        sendEquipmentForFakePlayer(fakeEntityId, attacker, viewers);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick <= LUNGE_TICKS) {
                    double progress = (double) tick / LUNGE_TICKS;
                    Location step = spawnAt.clone().add(targetPoint.clone().subtract(spawnAt).toVector().multiply(progress));
                    sendTeleportPacket(fakeEntityId, step, viewers);
                }

                if (tick == LUNGE_TICKS) {
                    sendSwingAnimation(fakeEntityId, viewers);
                }

                if (tick >= LUNGE_TICKS + LINGER_TICKS_AFTER_HIT) {
                    sendDestroyPacket(fakeEntityId, viewers);
                    removeFromTabList(fakeUuid, viewers);
                    cancel();
                    return;
                }

                tick++;
            }
        }.runTaskTimer(SoulsSMP.getInstance(), 0L, 1L);
    }

    private static void addToTabList(WrappedGameProfile profile, List<Player> viewers) {
        PacketContainer packet = manager().createPacket(PacketType.Play.Server.PLAYER_INFO);        packet.getPlayerInfoActions().write(0, Set.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
        packet.getPlayerInfoDataLists().write(
                1, // ADD_PLAYER's data list slot; some ProtocolLib versions use a different index here
                List.of(new com.comphenix.protocol.wrappers.PlayerInfoData(
                        profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, com.comphenix.protocol.wrappers.WrappedChatComponent.fromText(profile.getName())
                ))
        );
        sendToAll(packet, viewers);
    }

    private static void removeFromTabList(UUID fakeUuid, List<Player> viewers) {
        PacketContainer packet = manager().createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        packet.getUUIDLists().write(0, List.of(fakeUuid));
        sendToAll(packet, viewers);
    }

    private static void sendSpawnPacket(int entityId, UUID uuid, Location loc, List<Player> viewers) {
        PacketContainer packet = manager().createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, uuid);
        packet.getEntityTypeModifier().write(0, EntityType.PLAYER); // VERSION-SENSITIVE
        packet.getDoubles().write(0, loc.getX()).write(1, loc.getY()).write(2, loc.getZ());
        packet.getBytes().write(0, yawToByte(loc.getYaw())).write(1, pitchToByte(loc.getPitch()));
        sendToAll(packet, viewers);
    }

    private static void sendTeleportPacket(int entityId, Location loc, List<Player> viewers) {
        PacketContainer packet = manager().createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, entityId);
        packet.getDoubles().write(0, loc.getX()).write(1, loc.getY()).write(2, loc.getZ());
        packet.getBytes().write(0, yawToByte(loc.getYaw())).write(1, pitchToByte(loc.getPitch()));
        sendToAll(packet, viewers);
    }

    private static void sendSwingAnimation(int entityId, List<Player> viewers) {
        PacketContainer packet = manager().createPacket(PacketType.Play.Server.ANIMATION);
        packet.getIntegers().write(0, entityId);
        packet.getIntegers().write(1, 0); // 0 = swing main arm
        sendToAll(packet, viewers);
    }

    private static void sendSkinLayerMetadata(int entityId, List<Player> viewers) {
        PacketContainer packet = manager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entityId);
        // Index 17 = "player mode customisation" (skin layers). VERSION-SENSITIVE - double check if
        // the afterimage renders with missing sleeves/jacket/hat layers.
        WrappedDataValue skinLayers = new WrappedDataValue(17, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x7F);
        packet.getDataValueCollectionModifier().write(0, List.of(skinLayers));
        sendToAll(packet, viewers);
    }

    private static void sendEquipmentForFakePlayer(int entityId, Player attacker, List<Player> viewers) {
        EntityEquipment equipment = attacker.getEquipment();
        if (equipment == null) return;

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> slots = new ArrayList<>();
        slots.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, equipment.getHelmet()));
        slots.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, equipment.getChestplate()));
        slots.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, equipment.getLeggings()));
        slots.add(new Pair<>(EnumWrappers.ItemSlot.FEET, equipment.getBoots()));
        slots.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, equipment.getItemInMainHand()));

        PacketContainer packet = manager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, entityId);
        packet.getSlotStackPairLists().write(0, slots);
        sendToAll(packet, viewers);
    }

    private static void sendDestroyPacket(int entityId, List<Player> viewers) {
        PacketContainer packet = manager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntLists().write(0, List.of(entityId)); // VERSION-SENSITIVE: some versions use getIntegerArrays() instead
        sendToAll(packet, viewers);
    }

    private static void sendToAll(PacketContainer packet, List<Player> viewers) {
        for (Player viewer : viewers) {
            try {
                manager().sendServerPacket(viewer, packet);
            } catch (Exception e) {
                SoulsSMP.getInstance().getLogger().warning("Failed to send Echo afterimage packet: " + e.getMessage());
            }
        }
    }

    private static List<Player> nearbyViewers(Player subject) {
        List<Player> viewers = new ArrayList<>();
        for (Player online : subject.getWorld().getPlayers()) {
            if (online.getLocation().distance(subject.getLocation()) <= VIEW_RADIUS) {
                viewers.add(online);
            }
        }
        return viewers;
    }

    private static byte yawToByte(float yaw) {
        return (byte) (int) (yaw * 256.0F / 360.0F);
    }

    private static byte pitchToByte(float pitch) {
        return (byte) (int) (pitch * 256.0F / 360.0F);
    }
}