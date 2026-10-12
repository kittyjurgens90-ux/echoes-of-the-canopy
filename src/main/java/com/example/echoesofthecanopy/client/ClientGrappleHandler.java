package com.example.echoesofthecanopy.client;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.grapple.GrappleConstants;
import com.example.echoesofthecanopy.network.GrappleInputC2SPacket;
import com.example.echoesofthecanopy.network.GrappleSyncS2CPacket;
import com.example.echoesofthecanopy.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** CLIENT ONLY. Owns the local player's pull: smooth (no server round-trip per tick), server-validated. */
@Mod.EventBusSubscriber(modid = EchoesOfTheCanopy.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientGrappleHandler {
    private static boolean active;
    private static Vec3 anchor = Vec3.ZERO;
    private static int ticks;
    private static int stallTicks;
    private static Vec3 lastPos = Vec3.ZERO;

    /** Called from GrappleSyncS2CPacket (already on the client main thread). */
    public static void handleSync(GrappleSyncS2CPacket msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        switch (msg.action()) {
            case ATTACH -> {
                active = true;
                anchor = msg.vec();
                ticks = 0;
                stallTicks = 0;
                lastPos = player.position();
            }
            case DETACH -> {
                active = false;
                player.setDeltaMovement(msg.vec());
                player.fallDistance = 0.0F;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // START: velocity must be set before this tick's travel()/move() consumes it
        if (event.phase != TickEvent.Phase.START || !active) return;
        if (!(event.player instanceof LocalPlayer player)) return; // ignore RemotePlayers

        if (!player.isAlive() || player.isPassenger()) { active = false; return; }

        ticks++;
        Vec3 toAnchor = anchor.subtract(player.getEyePosition());
        double dist = toAnchor.length();

        stallTicks = player.position().distanceToSqr(lastPos) < 0.0025D ? stallTicks + 1 : 0;
        lastPos = player.position();

        Options opts = Minecraft.getInstance().options;
        boolean release = dist < GrappleConstants.ARRIVE_DISTANCE
                || ticks > GrappleConstants.MAX_PULL_TICKS
                || (ticks > 4 && (opts.keyJump.isDown() || opts.keyShift.isDown()))
                || (ticks > 6 && (player.horizontalCollision || stallTicks >= 4));

        if (release) {
            Vec3 v = player.getDeltaMovement();
            active = false;
            player.setDeltaMovement(GrappleConstants.exitVelocity(v));
            ModNetwork.sendToServer(new GrappleInputC2SPacket(v, true));
            return;
        }

        Vec3 desired = toAnchor.scale(GrappleConstants.PULL_SPEED / dist);
        Vec3 velocity = player.getDeltaMovement().lerp(desired, GrappleConstants.PULL_RESPONSE);
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
