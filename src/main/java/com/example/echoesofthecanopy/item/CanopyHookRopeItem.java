package com.example.echoesofthecanopy.item;

import com.example.echoesofthecanopy.entity.HookRopeEntity;
import com.example.echoesofthecanopy.grapple.GrappleConstants;
import com.example.echoesofthecanopy.grapple.GrappleManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click: fire the hook. Right-click again while a hook is out: retract it. */
public class CanopyHookRopeItem extends Item {
    public CanopyHookRopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            HookRopeEntity existing = GrappleManager.get(sp);
            if (existing != null) {
                existing.release(true);
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8F, 1.0F);
                return InteractionResultHolder.consume(stack);
            }

            HookRopeEntity hook = new HookRopeEntity(level, sp);
            hook.setPullOwnerToTarget(sp.isShiftKeyDown()); // sneaking at throw => player is pulled to a hooked mob
            hook.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, GrappleConstants.LAUNCH_SPEED, 0.0F);
            level.addFreshEntity(hook);
            GrappleManager.register(sp, hook);

            stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
            sp.getCooldowns().addCooldown(this, 10);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6F, 0.8F);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
