package com.example.echoesofthecanopy.registry;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.entity.HookRopeEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EchoesOfTheCanopy.MODID);

    public static final RegistryObject<EntityType<HookRopeEntity>> HOOK_ROPE =
            ENTITY_TYPES.register("hook_rope", () -> EntityType.Builder
                    .<HookRopeEntity>of(HookRopeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)      // fast projectile: sync every tick
                    .noSave()               // never persisted; no NBT handling needed
                    .noSummon()
                    .fireImmune()
                    .build(new ResourceLocation(EchoesOfTheCanopy.MODID, "hook_rope").toString()));

    private ModEntities() {}
}
