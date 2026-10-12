package com.example.echoesofthecanopy.network;

import com.example.echoesofthecanopy.client.ClientGrappleHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> owning client. ATTACH: vec = anchor point. DETACH: vec = exit velocity to apply. */
public record GrappleSyncS2CPacket(Action action, Vec3 vec) {
    public enum Action { ATTACH, DETACH }

    public static void encode(GrappleSyncS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeDouble(msg.vec.x);
        buf.writeDouble(msg.vec.y);
        buf.writeDouble(msg.vec.z);
    }

    public static GrappleSyncS2CPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        return new GrappleSyncS2CPacket(action, new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public static void handle(GrappleSyncS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientGrappleHandler.handleSync(msg)));
        ctx.get().setPacketHandled(true);
    }
}
