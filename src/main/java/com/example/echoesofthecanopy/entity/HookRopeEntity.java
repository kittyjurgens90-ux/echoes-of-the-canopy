package com.example.echoesofthecanopy.entity;

import com.example.echoesofthecanopy.grapple.GrappleConstants;
import com.example.echoesofthecanopy.grapple.GrappleManager;
import com.example.echoesofthecanopy.network.GrappleSyncS2CPacket;
import com.example.echoesofthecanopy.network.ModNetwork;
import com.example.echoesofthecanopy.registry.ModEntities;
import com.example.echoesofthecanopy.registry.ModItems;
import com.example.echoesofthecanopy.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

/**
 * Grapple projectile.
 *
 * Flight: no range or lifespan cap. It arcs under gravity until it hits a block, hits a LivingEntity, or
 * touches water/lava (vanilla also removes it below the world).
 *
 * Latched modes:
 *  BLOCK        - leaves/logs: the player is pulled to the anchor (client drives, server validates).
 *  PULL_PLAYER  - entity hit while the thrower was SNEAKING at throw time: the player is pulled to the mob.
 *  PULL_TARGET  - entity hit while standing: the mob is pulled to the player (server drives every tick).
 * Every mode keeps pulling each tick until arrival (~1.75 blocks). The only early exits are failure
 * conditions: target dead/removed, anchor gone, owner gone, or a genuine stall (no progress for 3 s).
 */
public class HookRopeEntity extends Projectile implements ItemSupplier {
    private enum Mode { FLYING, BLOCK, PULL_PLAYER, PULL_TARGET }

    private static final EntityDataAccessor<Boolean> DATA_LATCHED =
            SynchedEntityData.defineId(HookRopeEntity.class, EntityDataSerializers.BOOLEAN);

    private Mode mode = Mode.FLYING;
    private boolean pullOwnerToTarget;
    @Nullable private BlockPos anchorBlock;
    @Nullable private LivingEntity target;
    private Vec3 lastClientVelocity = Vec3.ZERO;
    private double bestDistance = Double.MAX_VALUE;
    private int stallTicks;
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

    /** Decided by the item at throw time: true = pull the PLAYER toward a hooked mob, false = pull the MOB. */
    public void setPullOwnerToTarget(boolean value) {
        this.pullOwnerToTarget = value;
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
    public boolean canChangeDimensions() {
        return false; // a hook drifting into a portal plate must not be teleported
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 160.0D * 160.0D; // matches the 10-chunk tracking range
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity instanceof LivingEntity && entity != getOwner() && super.canHitEntity(entity);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();

        if (isLatched()) {
            if (!level().isClientSide) serverTickLatched();
            return; // server moves a latched hook; clients receive its position
        }

        if (!level().isClientSide) {
            ServerPlayer owner = validOwner();
            if (owner == null) { discard(); return; }

            if (isInWater() || isInLava()) { // "falls and touches water"
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.4F, 1.4F);
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
        setDeltaMovement(m.x * 0.99D, m.y * 0.99D - GrappleConstants.GRAVITY, m.z * 0.99D);
    }

    // ------------------------------------------------------------- impacts

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide) return;

        ServerPlayer owner = validOwner();
        BlockState state = level().getBlockState(hit.getBlockPos());
        if (owner == null || !isGrappleable(state)) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.6F, 1.0F);
            discard(); // landed on ground / a non-latchable block
            return;
        }

        Vec3 loc = hit.getLocation();
        this.anchorBlock = hit.getBlockPos();
        this.mode = Mode.BLOCK;
        setPos(loc.x, loc.y, loc.z);
        beginHold();
        ModNetwork.sendToPlayer(owner, GrappleSyncS2CPacket.attachBlock(loc));
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide) return;

        ServerPlayer owner = validOwner();
        if (owner == null || !(hit.getEntity() instanceof LivingEntity living)
                || living.getType().is(ModTags.HOOK_IMMUNE)) {
            discard();
            return;
        }

        this.target = living;
        this.mode = pullOwnerToTarget ? Mode.PULL_PLAYER : Mode.PULL_TARGET;
        Vec3 c = center(living);
        setPos(c.x, c.y, c.z);
        beginHold();
        if (mode == Mode.PULL_PLAYER) {
            ModNetwork.sendToPlayer(owner, GrappleSyncS2CPacket.attachEntity(living.getId(), c));
        }
    }

    private void beginHold() {
        setDeltaMovement(Vec3.ZERO);
        this.entityData.set(DATA_LATCHED, true);
        this.lastClientVelocity = Vec3.ZERO;
        this.bestDistance = Double.MAX_VALUE;
        this.stallTicks = 0;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    // ------------------------------------------------- latched server logic

    private void serverTickLatched() {
        ServerPlayer owner = validOwner();
        if (owner == null) { discard(); return; }

        switch (mode) {
            case BLOCK -> tickBlock(owner);
            case PULL_PLAYER -> tickPullPlayer(owner);
            case PULL_TARGET -> tickPullTarget(owner);
            default -> discard();
        }
    }

    private void tickBlock(ServerPlayer owner) {
        if (anchorBlock == null || !isGrappleable(level().getBlockState(anchorBlock))) { // leaves can decay
            release(true);
            return;
        }
        double dist = owner.getEyePosition().distanceTo(position());
        if (dist < GrappleConstants.ARRIVE_DISTANCE || stalled(dist)) { release(true); return; }
        owner.fallDistance = 0.0F;
    }

    private void tickPullPlayer(ServerPlayer owner) {
        LivingEntity t = liveTarget();
        if (t == null) { release(true); return; }
        Vec3 c = center(t);
        setPos(c.x, c.y, c.z); // hook follows the mob

        double dist = owner.getEyePosition().distanceTo(c);
        if (dist < arriveRadius(t) || stalled(dist)) { release(true); return; }
        owner.fallDistance = 0.0F;
    }

    private void tickPullTarget(ServerPlayer owner) {
        LivingEntity t = liveTarget();
        if (t == null) { release(true); return; }
        Vec3 c = center(t);
        setPos(c.x, c.y, c.z);

        Vec3 toOwner = center(owner).subtract(c);
        double dist = toOwner.length();
        if (dist < arriveRadius(t) || stalled(dist)) { release(true); return; }

        // Continuous pull: re-applied EVERY tick until arrival, overriding AI/friction.
        Vec3 desired = toOwner.scale(GrappleConstants.PULL_SPEED / dist);
        Vec3 v = t.getDeltaMovement().lerp(desired, GrappleConstants.PULL_RESPONSE);
        if (t.horizontalCollision && t.onGround()) {
            v = new Vec3(v.x, 0.42D, v.z); // hop up ledges instead of grinding on them
        }
        t.setDeltaMovement(v);
        t.fallDistance = 0.0F;
        t.hurtMarked = true; // sync velocity (essential if the target is a player)
    }

    /** Progress-based failsafe. Resets whenever the distance genuinely shrinks. */
    private boolean stalled(double dist) {
        if (dist < bestDistance - GrappleConstants.PROGRESS_EPSILON) {
            bestDistance = dist;
            stallTicks = 0;
            return false;
        }
        return ++stallTicks > GrappleConstants.STALL_RELEASE_TICKS;
    }

    @Nullable
    private LivingEntity liveTarget() {
        return target != null && !target.isRemoved() && target.isAlive() && target.level() == level() ? target : null;
    }

    private static double arriveRadius(LivingEntity t) {
        return GrappleConstants.ARRIVE_DISTANCE + t.getBbWidth() * 0.5D;
    }

    private static Vec3 center(Entity e) {
        return e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D);
    }

    private boolean pullsOwner() {
        return mode == Mode.BLOCK || mode == Mode.PULL_PLAYER;
    }

    // -------------------------------------------------------- server API

    /** Called from GrappleInputC2SPacket (server thread). Never trust the payload. */
    public void acceptClientInput(ServerPlayer sender, Vec3 reported, boolean release) {
        if (!isLatched() || !pullsOwner() || sender != getOwner()) return;
        Vec3 safe = GrappleConstants.sanitize(reported);
        if (safe == null) { release(true); return; }

        this.lastClientVelocity = safe;
        sender.setDeltaMovement(safe);   // mirror only; NOT hurtMarked, so no echo packet to the client
        sender.fallDistance = 0.0F;
        if (release) release(false);     // client already applied its own exit velocity
    }

    /** @param notifyClient true when the SERVER ends the grapple (arrival, stall, decay, retract, logout...) */
    public void release(boolean notifyClient) {
        if (level().isClientSide) return;
        this.silentDetach = !notifyClient;
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            Entity owner = getOwner();
            if (isLatched() && pullsOwner() && !silentDetach
                    && owner instanceof ServerPlayer sp && !sp.hasDisconnected()) {
                Vec3 exit = GrappleConstants.exitVelocity(lastClientVelocity);
                sp.setDeltaMovement(exit);
                ModNetwork.sendToPlayer(sp, GrappleSyncS2CPacket.detach(exit));
            }
            silentDetach = true; // never send twice
            if (owner != null) GrappleManager.unregister(owner.getUUID(), this);
        }
        super.remove(reason);
    }

    @Nullable
    private ServerPlayer validOwner() {
        return getOwner() instanceof ServerPlayer sp && sp.isAlive() && !sp.hasDisconnected()
                && sp.level() == this.level() ? sp : null;
    }
}
