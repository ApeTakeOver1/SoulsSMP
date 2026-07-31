package net.ape.soulssmp.abilities.echo.passive;

import net.ape.soulssmp.abilities.echo.EchoPacketService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Entry point for Perfect Echo's attack afterimage. Delegates to
 * EchoPacketService, which spawns a real packet-level fake player (via
 * ProtocolLib) wearing the attacker's skin and gear, rather than an
 * ArmorStand puppet. If ProtocolLib isn't installed, this silently does
 * nothing (see EchoPacketService.isAvailable()).
 */
public class EchoAfterimage {

    private EchoAfterimage() {
    }

    public static void spawn(Player attacker, LivingEntity target) {
        EchoPacketService.spawnAfterimage(attacker, target);
    }
}