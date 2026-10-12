package com.example.echoesofthecanopy.client;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EchoesOfTheCanopy.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOOK_ROPE.get(), HookRopeRenderer::new);
    }

    private ClientSetup() {}
}
