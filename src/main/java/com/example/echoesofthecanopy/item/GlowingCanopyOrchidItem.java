package com.example.echoesofthecanopy.item;

import com.example.echoesofthecanopy.portal.CanopyPortalShape;
import com.example.echoesofthecanopy.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * Right-click a log that belongs to a valid log frame (interior 2x3 up to 21x21) to light the portal.
 * Sneak + right-click always plants the orchid instead. Only works in the Overworld and the Ever-Canopy.
 */
public class GlowingCanopyOrchidItem extends BlockItem {
    public GlowingCanopyOrchidItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos clicked = ctx.getClickedPos();

        boolean sneaking = player != null && player.isSecondaryUseActive();
        boolean validDimension = level.dimension() == Level.OVERWORLD
                || level.dimension() == ModDimensions.EVER_CANOPY_LEVEL;

        if (!sneaking && validDimension && level.getBlockState(clicked).is(BlockTags.LOGS)) {
            Optional<CanopyPortalShape> shape = CanopyPortalShape.findAround(level, clicked);
            if (shape.isPresent()) {
                if (!level.isClientSide) {
                    shape.get().createPortal(level);
                    level.playSound(null, clicked, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F);
                    if (player == null || !player.getAbilities().instabuild) {
                        ctx.getItemInHand().shrink(1);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useOn(ctx); // normal plant placement
    }
}
