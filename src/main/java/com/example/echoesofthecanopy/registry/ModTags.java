package com.example.echoesofthecanopy.registry;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    /** Entities the hook refuses to latch onto (data-driven: data/echoesofthecanopy/tags/entity_types/hook_immune.json). */
    public static final TagKey<EntityType<?>> HOOK_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(EchoesOfTheCanopy.MODID, "hook_immune"));

    private ModTags() {}
}
