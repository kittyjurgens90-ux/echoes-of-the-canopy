package com.example.echoesofthecanopy.portal;

import com.example.echoesofthecanopy.block.CanopyPortalBlock;
import com.example.echoesofthecanopy.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** A rectangular frame of logs (any log). Interior 2..21 wide, 3..21 tall, axis X or Z. Corners are optional. */
public final class CanopyPortalShape {
    public static final int MIN_WIDTH = 2, MAX_WIDTH = 21, MIN_HEIGHT = 3, MAX_HEIGHT = 21;

    private final Direction.Axis axis;
    private final Direction right;
    private final BlockPos bottomLeft;
    private final int width;
    private final int height;

    private CanopyPortalShape(Direction.Axis axis, BlockPos bottomLeft, int width, int height) {
        this.axis = axis;
        this.right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
    }

    /** Tries every neighbour of the clicked log as a possible interior cell, for both axes. */
    public static Optional<CanopyPortalShape> findAround(LevelReader level, BlockPos framePos) {
        for (Direction d : Direction.values()) {
            BlockPos start = framePos.relative(d);
            for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                Optional<CanopyPortalShape> shape = find(level, start, axis);
                if (shape.isPresent()) return shape;
            }
        }
        return Optional.empty();
    }

    public static Optional<CanopyPortalShape> find(LevelReader level, BlockPos start, Direction.Axis axis) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction left = right.getOpposite();
        if (!isEmpty(level.getBlockState(start))) return Optional.empty();

        // 1. slide down to the floor row
        BlockPos pos = start;
        int guard = 0;
        while (isEmpty(level.getBlockState(pos.below())) && guard++ < MAX_HEIGHT) pos = pos.below();
        if (!isFrame(level, pos.below())) return Optional.empty();

        // 2. slide left to the left wall
        guard = 0;
        while (isEmpty(level.getBlockState(pos.relative(left))) && isFrame(level, pos.relative(left).below())
                && guard++ < MAX_WIDTH) {
            pos = pos.relative(left);
        }
        if (!isFrame(level, pos.relative(left))) return Optional.empty();
        BlockPos bottomLeft = pos;

        // 3. measure width along the floor
        int w = 0;
        while (w <= MAX_WIDTH && isEmpty(level.getBlockState(bottomLeft.relative(right, w)))
                && isFrame(level, bottomLeft.relative(right, w).below())) {
            w++;
        }
        if (w < MIN_WIDTH || w > MAX_WIDTH || !isFrame(level, bottomLeft.relative(right, w))) return Optional.empty();

        // 4. measure height; both side walls must be logs on every row
        int h = 0;
        rows:
        for (; h < MAX_HEIGHT; h++) {
            for (int i = 0; i < w; i++) {
                if (!isEmpty(level.getBlockState(bottomLeft.relative(right, i).above(h)))) break rows;
            }
            if (!isFrame(level, bottomLeft.relative(left).above(h))
                    || !isFrame(level, bottomLeft.relative(right, w).above(h))) {
                return Optional.empty();
            }
        }
        if (h < MIN_HEIGHT) return Optional.empty();

        // 5. top row must be logs
        for (int i = 0; i < w; i++) {
            if (!isFrame(level, bottomLeft.relative(right, i).above(h))) return Optional.empty();
        }
        return Optional.of(new CanopyPortalShape(axis, bottomLeft, w, h));
    }

    public void createPortal(Level level) {
        BlockState portal = ModBlocks.CANOPY_PORTAL.get().defaultBlockState().setValue(CanopyPortalBlock.AXIS, axis);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                level.setBlock(bottomLeft.relative(right, i).above(j), portal, 2); // flag 2: no neighbour cascade
            }
        }
    }

    /** Builds a complete frame + lit portal. interiorBottomLeft = lowest-left INTERIOR cell. */
    public static void buildFrame(Level level, BlockPos interiorBottomLeft, Direction.Axis axis, int width, int height) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockState portal = ModBlocks.CANOPY_PORTAL.get().defaultBlockState().setValue(CanopyPortalBlock.AXIS, axis);
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        for (int i = -1; i <= width; i++) {
            for (int j = -1; j <= height; j++) {
                BlockPos p = interiorBottomLeft.relative(right, i).above(j);
                boolean sideColumn = i == -1 || i == width;
                boolean row = j == -1 || j == height;
                if (sideColumn || row) {
                    level.setBlock(p, log.setValue(RotatedPillarBlock.AXIS, row ? axis : Direction.Axis.Y), 2);
                } else {
                    level.setBlock(p, portal, 2);
                }
            }
        }
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE);
    }

    private static boolean isFrame(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.LOGS);
    }
}
