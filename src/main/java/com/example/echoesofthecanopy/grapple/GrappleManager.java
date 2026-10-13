package com.example.echoesofthecanopy.grapple;

import com.example.echoesofthecanopy.entity.HookRopeEntity;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player UUID -> active hook. SERVER THREAD ONLY (item use, entity tick, enqueued packet work and
 * Forge events all run there), so a plain HashMap is correct and a ConcurrentHashMap would only hide bugs.
 */
public final class GrappleManager {
    private static final Map<UUID, HookRopeEntity> ACTIVE = new HashMap<>();

    @Nullable
    public static HookRopeEntity get(ServerPlayer player) {
        HookRopeEntity hook = ACTIVE.get(player.getUUID());
        if (hook != null && hook.isRemoved()) {
            ACTIVE.remove(player.getUUID(), hook);
            return null;
        }
        return hook;
    }

    public static void register(ServerPlayer player, HookRopeEntity hook) {
        HookRopeEntity old = ACTIVE.put(player.getUUID(), hook);
        if (old != null && old != hook && !old.isRemoved()) old.release(true);
    }

    /** Conditional remove: a stale hook can never evict its replacement. */
    public static void unregister(UUID playerId, HookRopeEntity hook) {
        ACTIVE.remove(playerId, hook);
    }

    public static void releaseFor(ServerPlayer player) {
        HookRopeEntity hook = ACTIVE.get(player.getUUID());
        if (hook != null) hook.release(true);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private GrappleManager() {}
}
