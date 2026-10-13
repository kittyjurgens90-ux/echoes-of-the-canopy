package com.example.echoesofthecanopy.network;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Flow:
 *  1. Server: hook latches onto block/entity -> S2C GrappleSync(ATTACH_BLOCK/ATTACH_ENTITY) (owner only, only when the PLAYER is pulled)
 *  2. Client: pulls itself each tick  -> C2S GrappleInput(velocity, false) every 2 ticks (server validates + mirrors)
 *  3. Client: arrives/jumps/sneaks    -> C2S GrappleInput(velocity, true)  (client applies exit velocity locally)
 *  4. Server-initiated end (timeout, decayed leaves, retract, unequip) -> S2C GrappleSync(DETACH, exitVelocity)
 */
public final class ModNetwork {
    private static final String PROTOCOL = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(EchoesOfTheCanopy.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, GrappleSyncS2CPacket.class,
                GrappleSyncS2CPacket::encode, GrappleSyncS2CPacket::decode, GrappleSyncS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, GrappleInputC2SPacket.class,
                GrappleInputC2SPacket::encode, GrappleInputC2SPacket::decode, GrappleInputC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    private ModNetwork() {}
}
