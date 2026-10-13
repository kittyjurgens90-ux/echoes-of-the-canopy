package com.example.echoesofthecanopy.portal;

import com.example.echoesofthecanopy.block.CanopyPortalBlock;
import com.example.echoesofthecanopy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/**
 * Coordinates map 1:1 between dimensions. Entering: reuse a portal near (x,z) or build the arrival platform.
 * Leaving: reuse a portal near (x,z) in the Overworld or build a frame on the surface.
 * getPortalInfo is overridden because vanilla's default entry-point lookup returns null for non-Nether/End
 * destinations, which would abort the teleport.
 */
public final class CanopyPortalTeleporter implements ITeleporter {
    private final BlockPos origin;
    private final boolean enteringCanopy;

    public CanopyPortalTeleporter(BlockPos origin, boolean enteringCanopy) {
        this.origin = origin;
        this.enteringCanopy = enteringCanopy;
    }

    private record Placement(Vec3 pos, float yaw) {}

    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerLevel dest, Function<ServerLevel, PortalInfo> defaultInfo) {
        Placement p = enteringCanopy ? arrival(dest) : returnTrip(dest);
        return new PortalInfo(p.pos(), Vec3.ZERO, p.yaw(), 0.0F);
    }

    @Override
    public Entity placeEntity(Entity entity, ServerLevel current, ServerLevel dest, float yaw,
                              Function<Boolean, Entity> repositionEntity) {
        Entity placed = repositionEntity.apply(false);
        if (placed != null) placed.setPortalCooldown();
        return placed;
    }

    @Override
    public boolean playTeleportSound(ServerPlayer player, ServerLevel source, ServerLevel dest) {
        return false;
    }

    private Placement arrival(ServerLevel dest) {
        int cx = origin.getX(), cz = origin.getZ();
        BlockPos found = CanopyPortalBuilder.findPortal(dest, cx, cz,
                CanopyPortalBuilder.ARRIVAL_Y - 6, CanopyPortalBuilder.ARRIVAL_Y + 12);
        if (found != null) return inFront(dest, found);
        CanopyPortalBuilder.buildArrival(dest, cx, cz);
        return new Placement(new Vec3(cx + 1.0D, CanopyPortalBuilder.ARRIVAL_Y, cz + 0.5D), 180.0F);
    }

    private Placement returnTrip(ServerLevel dest) {
        int cx = origin.getX(), cz = origin.getZ();
        int surface = dest.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
        BlockPos found = CanopyPortalBuilder.findPortal(dest, cx, cz, surface - 12, surface + 16);
        if (found != null) return inFront(dest, found);
        CanopyPortalBuilder.buildReturn(dest, cx, cz, surface);
        return new Placement(new Vec3(cx + 1.0D, surface, cz + 0.5D), 180.0F);
    }

    /** Stand one block in front of the lowest portal cell of the column, facing it. */
    private static Placement inFront(ServerLevel level, BlockPos found) {
        BlockState state = level.getBlockState(found);
        Direction.Axis axis = state.getValue(CanopyPortalBlock.AXIS);
        BlockPos low = found;
        while (level.getBlockState(low.below()).is(ModBlocks.CANOPY_PORTAL.get())) low = low.below();

        Direction normal = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        BlockPos front = low.relative(normal);
        if (!level.getBlockState(front).getCollisionShape(level, front).isEmpty()) {
            normal = normal.getOpposite();
            front = low.relative(normal);
            if (!level.getBlockState(front).getCollisionShape(level, front).isEmpty()) front = low;
        }
        return new Placement(Vec3.atBottomCenterOf(front), normal.getOpposite().toYRot());
    }
}
