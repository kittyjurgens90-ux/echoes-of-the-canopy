package com.example.echoesofthecanopy.portal;

import com.example.echoesofthecanopy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Finds existing exit portals and builds arrival structures (blocks set with flag 2: no update cascades). */
public final class CanopyPortalBuilder {
    public static final int ARRIVAL_Y = 112;     // feet level of the arrival platform in the Ever-Canopy
    public static final int SEARCH_RADIUS = 8;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState LOG = Blocks.DARK_OAK_LOG.defaultBlockState();

    @Nullable
    public static BlockPos findPortal(ServerLevel level, int cx, int cz, int minY, int maxY) {
        int lo = Math.max(minY, level.getMinBuildHeight());
        int hi = Math.min(maxY, level.getMaxBuildHeight() - 1);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                double dist = dx * dx + dz * dz;
                if (dist >= bestDist) continue;
                for (int y = lo; y <= hi; y++) {
                    m.set(cx + dx, y, cz + dz);
                    if (level.getBlockState(m).is(ModBlocks.CANOPY_PORTAL.get())) {
                        best = m.immutable();
                        bestDist = dist;
                        break;
                    }
                }
            }
        }
        return best;
    }

    /** Round grass-over-dirt-over-log platform with a portal on its north side and a few lit orchids. */
    public static void buildArrival(ServerLevel level, int cx, int cz) {
        int floorY = ARRIVAL_Y - 1;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz > 20) continue;
                BlockPos top = new BlockPos(cx + dx, floorY, cz + dz);
                set(level, top, GRASS);
                set(level, top.below(), DIRT);
                set(level, top.below(2), LOG);
                set(level, top.below(3), LOG);
                for (int dy = 1; dy <= 6; dy++) set(level, top.above(dy), AIR);
            }
        }
        CanopyPortalShape.buildFrame(level, new BlockPos(cx, ARRIVAL_Y, cz - 2), Direction.Axis.X, 2, 3);

        BlockState orchid = ModBlocks.GLOWING_CANOPY_ORCHID.get().defaultBlockState();
        set(level, new BlockPos(cx - 3, ARRIVAL_Y, cz + 1), orchid);
        set(level, new BlockPos(cx + 4, ARRIVAL_Y, cz + 1), orchid);
        set(level, new BlockPos(cx, ARRIVAL_Y, cz + 4), orchid);
    }

    /** Frame standing on the surface, with the interior floor flush with the ground. */
    public static void buildReturn(ServerLevel level, int cx, int cz, int surfaceY) {
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -3; dz <= 1; dz++) {
                for (int dy = 0; dy <= 4; dy++) set(level, new BlockPos(cx + dx, surfaceY + dy, cz + dz), AIR);
                BlockPos floor = new BlockPos(cx + dx, surfaceY - 1, cz + dz);
                if (!level.getBlockState(floor).blocksMotion()) set(level, floor, GRASS);
            }
        }
        CanopyPortalShape.buildFrame(level, new BlockPos(cx, surfaceY, cz - 2), Direction.Axis.X, 2, 3);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }

    private CanopyPortalBuilder() {}
}
