package com.example.echoesofthecanopy.portal;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dimension changes are deferred to the END of the server tick: never teleport mid-movement. SERVER THREAD ONLY. */
@Mod.EventBusSubscriber(modid = EchoesOfTheCanopy.MODID)
public final class PortalEvents {
    private record Pending(Entity entity, BlockPos portalPos) {}

    private static final Map<UUID, Pending> PENDING = new LinkedHashMap<>();

    public static void queue(Entity entity, BlockPos portalPos) {
        PENDING.putIfAbsent(entity.getUUID(), new Pending(entity, portalPos.immutable()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        List<Pending> work = new ArrayList<>(PENDING.values());
        PENDING.clear();

        for (Pending p : work) {
            Entity entity = p.entity();
            if (entity.isRemoved() || entity.isOnPortalCooldown() || !(entity.level() instanceof ServerLevel source)) continue;

            boolean toCanopy = source.dimension() != ModDimensions.EVER_CANOPY_LEVEL;
            ResourceKey<Level> destKey = toCanopy ? ModDimensions.EVER_CANOPY_LEVEL : Level.OVERWORLD;
            ServerLevel dest = source.getServer().getLevel(destKey);
            if (dest == null) continue;

            entity.changeDimension(dest, new CanopyPortalTeleporter(p.portalPos(), toCanopy));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    private PortalEvents() {}
}
