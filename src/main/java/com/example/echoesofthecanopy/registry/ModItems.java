package com.example.echoesofthecanopy.registry;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.item.CanopyHookRopeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EchoesOfTheCanopy.MODID);

    public static final RegistryObject<Item> CANOPY_HOOK_ROPE =
            ITEMS.register("canopy_hook_rope", () -> new CanopyHookRopeItem(new Item.Properties().durability(250)));

    public static final RegistryObject<Item> GLOWING_CANOPY_ORCHID =
            ITEMS.register("glowing_canopy_orchid",
                    () -> new BlockItem(ModBlocks.GLOWING_CANOPY_ORCHID.get(), new Item.Properties()));

    private ModItems() {}
}
