package com.example.echoesofthecanopy.registry;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.block.GlowingCanopyOrchidBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EchoesOfTheCanopy.MODID);

    public static final RegistryObject<GlowingCanopyOrchidBlock> GLOWING_CANOPY_ORCHID =
            BLOCKS.register("glowing_canopy_orchid", () -> new GlowingCanopyOrchidBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .noCollission()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .lightLevel(state -> 12)
                            .offsetType(BlockBehaviour.OffsetType.XZ)
                            .pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {}
}
