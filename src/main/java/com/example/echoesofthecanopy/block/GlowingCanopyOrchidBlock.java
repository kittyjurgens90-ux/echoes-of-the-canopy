package com.example.echoesofthecanopy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Bioluminescent plant, light 12 (set in ModBlocks).
 *  hanging=false: stands on leaves or grass blocks.
 *  hanging=true : hangs from the underside of leaves or logs (placed by clicking the underside face).
 */
public class GlowingCanopyOrchidBlock extends BushBlock {
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    protected static final VoxelShape STANDING_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 12.0D, 13.0D);
    protected static final VoxelShape HANGING_SHAPE = Block.box(3.0D, 4.0D, 3.0D, 13.0D, 16.0D, 13.0D);

    public GlowingCanopyOrchidBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HANGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.LEAVES) || state.is(Blocks.GRASS_BLOCK);
    }

    /**
     * Overridden on purpose: Forge's BushBlock#canSurvive routes placed blocks through canSustainPlant(),
     * which would reject leaves. One rule everywhere (placement, neighbour updates, worldgen).
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HANGING)) {
            BlockState above = level.getBlockState(pos.above());
            return above.is(BlockTags.LEAVES) || above.is(BlockTags.LOGS);
        }
        BlockPos below = pos.below();
        return mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean preferHanging = ctx.getClickedFace() == Direction.DOWN;
        BlockState preferred = defaultBlockState().setValue(HANGING, preferHanging);
        if (preferred.canSurvive(ctx.getLevel(), ctx.getClickedPos())) return preferred;
        BlockState other = defaultBlockState().setValue(HANGING, !preferHanging);
        return other.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? other : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Vec3 o = state.getOffset(level, pos);
        return (state.getValue(HANGING) ? HANGING_SHAPE : STANDING_SHAPE).move(o.x, o.y, o.z);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) return;
        Vec3 o = state.getOffset(level, pos);
        double baseY = state.getValue(HANGING) ? 0.2D : 0.4D;
        level.addParticle(ParticleTypes.GLOW,
                pos.getX() + 0.5D + o.x + (random.nextDouble() - 0.5D) * 0.5D,
                pos.getY() + baseY + random.nextDouble() * 0.5D,
                pos.getZ() + 0.5D + o.z + (random.nextDouble() - 0.5D) * 0.5D,
                0.0D, 0.01D, 0.0D);
    }
}
