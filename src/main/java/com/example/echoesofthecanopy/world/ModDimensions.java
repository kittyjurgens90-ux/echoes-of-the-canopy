package com.example.echoesofthecanopy.world;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Keys for the datapack-driven dimension. The actual definitions live in JSON:
 *   data/echoesofthecanopy/dimension_type/ever_canopy.json
 *   data/echoesofthecanopy/dimension/ever_canopy.json
 *   data/echoesofthecanopy/worldgen/{noise_settings,noise,density_function,biome,configured_feature,placed_feature}/...
 */
public final class ModDimensions {
    private static final ResourceLocation ID = new ResourceLocation(EchoesOfTheCanopy.MODID, "ever_canopy");

    public static final ResourceKey<Level> EVER_CANOPY_LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
    public static final ResourceKey<DimensionType> EVER_CANOPY_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<Biome> EVER_CANOPY_FOREST = ResourceKey.create(Registries.BIOME,
            new ResourceLocation(EchoesOfTheCanopy.MODID, "ever_canopy_forest"));

    private ModDimensions() {}
}
