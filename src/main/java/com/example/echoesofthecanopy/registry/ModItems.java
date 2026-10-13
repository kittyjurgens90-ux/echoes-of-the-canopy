package com.example.echoesofthecanopy.registry;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.item.CanopyHookRopeItem;
import com.example.echoesofthecanopy.item.GlowingCanopyOrchidItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EchoesOfTheCanopy.MODID);

    public static final RegistryObject<Item> CANOPY_HOOK_ROPE =
            ITEMS.register("canopy_hook_rope", () -> new CanopyHookRopeItem(new Item.Properties().durability(250)));

    /** Plants as a block AND lights the Ever-Canopy portal when used on a log frame. */
    public static final RegistryObject<Item> GLOWING_CANOPY_ORCHID =
            ITEMS.register("glowing_canopy_orchid",
                    () -> new GlowingCanopyOrchidItem(ModBlocks.GLOWING_CANOPY_ORCHID.get(), new Item.Properties()));

    private ModItems() {}
}
