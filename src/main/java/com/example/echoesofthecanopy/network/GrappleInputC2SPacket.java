package com.example.echoesofthecanopy.network;

import com.example.echoesofthecanopy.entity.HookRopeEntity;
import com.example.echoesofthecanopy.grapple.GrappleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server. The client's current pull velocity, plus whether it just released the rope. */
public record GrappleInputC2SPacket(Vec3 velocity, boolean release) {

    public static void encode(GrappleInputC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.velocity.x);
        buf.writeDouble(msg.velocity.y);
        buf.writeDouble(msg.velocity.z);
        buf.writeBoolean(msg.release);
    }

    public static GrappleInputC2SPacket decode(FriendlyByteBuf buf) {
        return new GrappleInputC2SPacket(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readBoolean());
    }

    public static void handle(GrappleInputC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            HookRopeEntity hook = GrappleManager.get(sender);   // sender is derived server-side, never from the payload
            if (hook != null) hook.acceptClientInput(sender, msg.velocity, msg.release);
        });
        ctx.get().setPacketHandled(true);
    }
}
