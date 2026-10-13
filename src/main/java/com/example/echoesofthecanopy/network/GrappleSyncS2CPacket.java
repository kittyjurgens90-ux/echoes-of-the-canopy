package com.example.echoesofthecanopy.network;

import com.example.echoesofthecanopy.client.ClientGrappleHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> owning client.
 *  ATTACH_BLOCK : vec = anchor point on the block
 *  ATTACH_ENTITY: entityId = hooked entity, vec = its current centre (initial anchor); client tracks it every tick
 *  DETACH       : vec = exit velocity to apply (only if the client is still pulling)
 */
public record GrappleSyncS2CPacket(Action action, Vec3 vec, int entityId) {
    public enum Action { ATTACH_BLOCK, ATTACH_ENTITY, DETACH }

    public static GrappleSyncS2CPacket attachBlock(Vec3 anchor) {
        return new GrappleSyncS2CPacket(Action.ATTACH_BLOCK, anchor, -1);
    }

    public static GrappleSyncS2CPacket attachEntity(int entityId, Vec3 center) {
        return new GrappleSyncS2CPacket(Action.ATTACH_ENTITY, center, entityId);
    }

    public static GrappleSyncS2CPacket detach(Vec3 exitVelocity) {
        return new GrappleSyncS2CPacket(Action.DETACH, exitVelocity, -1);
    }

    public static void encode(GrappleSyncS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeDouble(msg.vec.x);
        buf.writeDouble(msg.vec.y);
        buf.writeDouble(msg.vec.z);
        buf.writeInt(msg.entityId);
    }

    public static GrappleSyncS2CPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        Vec3 vec = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new GrappleSyncS2CPacket(action, vec, buf.readInt());
    }

    public static void handle(GrappleSyncS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientGrappleHandler.handleSync(msg)));
        ctx.get().setPacketHandled(true);
    }
}
