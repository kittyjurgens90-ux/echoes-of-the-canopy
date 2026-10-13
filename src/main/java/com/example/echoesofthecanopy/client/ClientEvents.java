package com.example.echoesofthecanopy.client;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.world.ModDimensions;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** CLIENT ONLY. Thick mist in the Ever-Canopy (sky/fog TINT comes from the biome JSON). */
@Mod.EventBusSubscriber(modid = EchoesOfTheCanopy.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private static final float MIST_NEAR = 4.0F;
    private static final float MIST_FAR = 96.0F;

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.level.dimension() != ModDimensions.EVER_CANOPY_LEVEL) return;
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        if (event.getCamera().getFluidInCamera() != FogType.NONE) return; // keep water/lava fog
        if (mc.player.hasEffect(MobEffects.BLINDNESS)) return;

        event.setNearPlaneDistance(MIST_NEAR);
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), MIST_FAR));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true); // required for the changes to apply
    }

    private ClientEvents() {}
}
