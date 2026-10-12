package com.example.echoesofthecanopy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bioluminescent plant (light 12, set in ModBlocks) that survives on leaves or grass blocks. */
public class GlowingCanopyOrchidBlock extends BushBlock {
    protected static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 12.0D, 13.0D);

    public GlowingCanopyOrchidBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.LEAVES) || state.is(Blocks.GRASS_BLOCK);
    }

    /**
     * Overridden on purpose: Forge's BushBlock#canSurvive routes already-placed blocks through
     * canSustainPlant(), which would reject leaves for this plant type. We use one rule everywhere.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) return;
        Vec3 o = state.getOffset(level, pos);
        level.addParticle(ParticleTypes.GLOW,
                pos.getX() + 0.5D + o.x + (random.nextDouble() - 0.5D) * 0.5D,
                pos.getY() + 0.4D + random.nextDouble() * 0.5D,
                pos.getZ() + 0.5D + o.z + (random.nextDouble() - 0.5D) * 0.5D,
                0.0D, 0.01D, 0.0D);
    }
}
