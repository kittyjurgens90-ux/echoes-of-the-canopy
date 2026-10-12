package com.example.echoesofthecanopy.entity;

import com.example.echoesofthecanopy.grapple.GrappleConstants;
import com.example.echoesofthecanopy.grapple.GrappleManager;
import com.example.echoesofthecanopy.network.GrappleSyncS2CPacket;
import com.example.echoesofthecanopy.network.ModNetwork;
import com.example.echoesofthecanopy.registry.ModEntities;
import com.example.echoesofthecanopy.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

/**
 * Grapple projectile. Server is authoritative for the hook (flight, hit, latch validity, lifetime);
 * the owning client is authoritative for its own movement (vanilla player movement is client-side)
 * and the server validates/clamps what the client reports. See ModNetwork for the packet flow.
 */
public class HookRopeEntity extends Projectile implements ItemSupplier {
    private static final EntityDataAccessor<Boolean> DATA_LATCHED =
            SynchedEntityData.defineId(HookRopeEntity.class, EntityDataSerializers.BOOLEAN);

    private int flightTicks;
    private int pullTicks;
    @Nullable private BlockPos anchorBlock;
    private Vec3 lastClientVelocity = Vec3.ZERO;
    /** true -> don't send a DETACH packet on removal (the client already released itself). */
    private boolean silentDetach;

    public HookRopeEntity(EntityType<? extends HookRopeEntity> type, Level level) {
        super(type, level);
        this.noCulling = true; // the rope must stay visible even if the tiny hook is off-screen
    }

    public HookRopeEntity(Level level, ServerPlayer owner) {
        this(ModEntities.HOOK_ROPE.get(), level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    public static boolean isGrappleable(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS);
    }

    public boolean isLatched() {
        return this.entityData.get(DATA_LATCHED);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LATCHED, false);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.CANOPY_HOOK_ROPE.get());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        // default is ~16 blocks for a 0.25 box; the hook must render across its whole range
        return distSqr < 128.0D * 128.0D;
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return false; // blocks only
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();

        if (isLatched()) {
            if (!level().isClientSide) serverTickLatched();
            return; // stationary once latched, both sides
        }

        if (!level().isClientSide) {
            ServerPlayer owner = validOwner();
            if (owner == null || !isHoldingHook(owner)
                    || ++flightTicks > GrappleConstants.MAX_FLIGHT_TICKS
                    || distanceToSqr(owner) > GrappleConstants.MAX_RANGE * GrappleConstants.MAX_RANGE) {
                discard();
                return;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
                onHit(hit);
                if (isRemoved() || isLatched()) return;
            }
        }

        // Same integration on both sides => smooth client interpolation; server corrections still apply.
        Vec3 m = getDeltaMovement();
        setPos(getX() + m.x, getY() + m.y, getZ() + m.z);
        setDeltaMovement(m.x * 0.99D, m.y - 0.02D, m.z * 0.99D);
    }

    private void serverTickLatched() {
        ServerPlayer owner = validOwner();
        if (owner == null) { discard(); return; }
        if (anchorBlock == null
                || !isGrappleable(level().getBlockState(anchorBlock))   // leaves can decay mid-pull
                || ++pullTicks > GrappleConstants.MAX_PULL_TICKS
                || !isHoldingHook(owner)) {
            release(true);
            return;
        }
        owner.fallDistance = 0.0F;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide) return;

        ServerPlayer owner = validOwner();
        BlockState state = level().getBlockState(hit.getBlockPos());
        if (owner == null || !isGrappleable(state)) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.6F, 1.0F);
            discard();
            return;
        }

        Vec3 loc = hit.getLocation();
        this.anchorBlock = hit.getBlockPos();
        setPos(loc.x, loc.y, loc.z);
        setDeltaMovement(Vec3.ZERO);
        this.entityData.set(DATA_LATCHED, true);
        this.pullTicks = 0;
        this.lastClientVelocity = Vec3.ZERO;

        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.2F);
        ModNetwork.sendToPlayer(owner, new GrappleSyncS2CPacket(GrappleSyncS2CPacket.Action.ATTACH, loc));
    }

    // -------------------------------------------------------- server API

    /** Called from GrappleInputC2SPacket (server thread). Never trust the payload. */
    public void acceptClientInput(ServerPlayer sender, Vec3 reported, boolean release) {
        if (!isLatched() || sender != getOwner()) return;
        Vec3 safe = GrappleConstants.sanitize(reported);
        if (safe == null) { release(true); return; }

        this.lastClientVelocity = safe;
        sender.setDeltaMovement(safe);   // mirror only; NOT hurtMarked, so no echo packet to the client
        sender.fallDistance = 0.0F;
        if (release) release(false);     // client already applied its own exit velocity
    }

    /** @param notifyClient true when the SERVER ends the grapple (timeout, decay, retract, logout...) */
    public void release(boolean notifyClient) {
        if (level().isClientSide) return;
        this.silentDetach = !notifyClient;
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            net.minecraft.world.entity.Entity owner = getOwner();
            if (isLatched() && !silentDetach && owner instanceof ServerPlayer sp && !sp.hasDisconnected()) {
                Vec3 exit = GrappleConstants.exitVelocity(lastClientVelocity);
                sp.setDeltaMovement(exit);
                ModNetwork.sendToPlayer(sp, new GrappleSyncS2CPacket(GrappleSyncS2CPacket.Action.DETACH, exit));
            }
            silentDetach = true; // never send twice
            if (owner != null) GrappleManager.unregister(owner.getUUID(), this);
        }
        super.remove(reason);
    }

    // --------------------------------------------------------- helpers

    @Nullable
    private ServerPlayer validOwner() {
        return getOwner() instanceof ServerPlayer sp && sp.isAlive() && !sp.hasDisconnected()
                && sp.level() == this.level() ? sp : null;
    }

    private static boolean isHoldingHook(ServerPlayer p) {
        return p.getMainHandItem().is(ModItems.CANOPY_HOOK_ROPE.get())
                || p.getOffhandItem().is(ModItems.CANOPY_HOOK_ROPE.get());
    }
}
