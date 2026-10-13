package com.example.echoesofthecanopy.registry;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EchoesOfTheCanopy.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + EchoesOfTheCanopy.MODID))
                    .icon(() -> new ItemStack(ModItems.CANOPY_HOOK_ROPE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.CANOPY_HOOK_ROPE.get());
                        output.accept(ModItems.GLOWING_CANOPY_ORCHID.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
