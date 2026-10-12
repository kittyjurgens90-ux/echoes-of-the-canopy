package com.example.echoesofthecanopy;

import com.example.echoesofthecanopy.network.ModNetwork;
import com.example.echoesofthecanopy.registry.ModBlocks;
import com.example.echoesofthecanopy.registry.ModCreativeTabs;
import com.example.echoesofthecanopy.registry.ModEntities;
import com.example.echoesofthecanopy.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoesOfTheCanopy.MODID)
public class EchoesOfTheCanopy {
    public static final String MODID = "echoesofthecanopy";

    public EchoesOfTheCanopy() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
