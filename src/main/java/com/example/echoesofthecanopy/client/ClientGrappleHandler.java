package com.example.echoesofthecanopy.client;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.grapple.GrappleConstants;
import com.example.echoesofthecanopy.network.GrappleInputC2SPacket;
import com.example.echoesofthecanopy.network.GrappleSyncS2CPacket;
import com.example.echoesofthecanopy.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * CLIENT ONLY. Owns the local player's pull (block anchors AND hooked mobs): smooth, server-validated.
 * Only JUMP releases manually -- sneak is deliberately free because a sneak-throw starts the mob pull.
 */
@Mod.EventBusSubscriber(modid = EchoesOfTheCanopy.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientGrappleHandler {
    private static boolean active;
    private static int anchorEntityId = -1;          // >= 0 -> chasing a mob
    private static Vec3 anchor = Vec3.ZERO;
    private static double arriveRadius = GrappleConstants.ARRIVE_DISTANCE;
    private static int ticks;
    private static int stallTicks;
    private static Vec3 lastPos = Vec3.ZERO;

    /** Called from GrappleSyncS2CPacket (already on the client main thread). */
    public static void handleSync(GrappleSyncS2CPacket msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        switch (msg.action()) {
            case ATTACH_BLOCK -> begin(player, msg.vec(), -1);
            case ATTACH_ENTITY -> begin(player, msg.vec(), msg.entityId());
            case DETACH -> {
                if (!active) return; // client already released itself; don't clobber its velocity
                active = false;
                player.setDeltaMovement(msg.vec());
                player.fallDistance = 0.0F;
            }
        }
    }

    private static void begin(LocalPlayer player, Vec3 anchorPoint, int entityId) {
        active = true;
        anchor = anchorPoint;
        anchorEntityId = entityId;
        arriveRadius = GrappleConstants.ARRIVE_DISTANCE;
        ticks = 0;
        stallTicks = 0;
        lastPos = player.position();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // START: velocity must be set before this tick's travel()/move() consumes it
        if (event.phase != TickEvent.Phase.START || !active) return;
        if (!(event.player instanceof LocalPlayer player)) return; // ignore RemotePlayers

        if (!player.isAlive() || player.isPassenger()) { active = false; return; }

        boolean entityMode = anchorEntityId >= 0;
        if (entityMode) { // the mob moves: re-aim every tick
            Entity e = player.level().getEntity(anchorEntityId);
            if (e != null && !e.isRemoved()) {
                anchor = e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D);
                arriveRadius = GrappleConstants.ARRIVE_DISTANCE + e.getBbWidth() * 0.5D;
            }
        }

        ticks++;
        Vec3 toAnchor = anchor.subtract(player.getEyePosition());
        double dist = toAnchor.length();

        stallTicks = player.position().distanceToSqr(lastPos) < 0.0025D ? stallTicks + 1 : 0;
        lastPos = player.position();

        Options opts = Minecraft.getInstance().options;
        boolean release = dist < arriveRadius
                || (ticks > 4 && opts.keyJump.isDown())
                // Block anchors: running into the canopy IS arrival. Mob chases never self-release on
                // collision; the server's progress-based stall check is the only failsafe there.
                || (!entityMode && ticks > 6 && (player.horizontalCollision || stallTicks >= 4));

        if (release) {
            Vec3 v = player.getDeltaMovement();
            active = false;
            player.setDeltaMovement(GrappleConstants.exitVelocity(v));
            ModNetwork.sendToServer(new GrappleInputC2SPacket(v, true));
            return;
        }

        Vec3 desired = toAnchor.scale(GrappleConstants.PULL_SPEED / Math.max(dist, 1.0E-4D));
        Vec3 velocity = player.getDeltaMovement().lerp(desired, GrappleConstants.PULL_RESPONSE);
        if (entityMode && player.horizontalCollision && player.onGround()) {
            velocity = new Vec3(velocity.x, Math.max(velocity.y, 0.42D), velocity.z); // hop ledges
        }
        player.setDeltaMovement(velocity);
        player.fallDistance = 0.0F;

        if ((ticks & 1) == 0) { // throttle: 10 packets/second
            ModNetwork.sendToServer(new GrappleInputC2SPacket(velocity, false));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
    }

    private ClientGrappleHandler() {}
}
