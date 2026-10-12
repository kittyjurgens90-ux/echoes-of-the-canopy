package com.example.echoesofthecanopy.datagen;

import com.example.echoesofthecanopy.EchoesOfTheCanopy;
import com.example.echoesofthecanopy.registry.ModBlocks;
import com.example.echoesofthecanopy.registry.ModItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Run `./gradlew runData` -> output lands in src/generated/resources. */
@Mod.EventBusSubscriber(modid = EchoesOfTheCanopy.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModDataGenerators {
    private static final String MODID = EchoesOfTheCanopy.MODID;

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();

        gen.addProvider(event.includeClient(), new ModBlockStates(out, files));
        gen.addProvider(event.includeClient(), new ModItemModels(out, files));
        gen.addProvider(event.includeClient(), new ModLang(out));
        gen.addProvider(event.includeServer(), new ModRecipes(out));
        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK))));
    }

    static final class ModBlockStates extends BlockStateProvider {
        ModBlockStates(PackOutput out, ExistingFileHelper files) { super(out, MODID, files); }

        @Override
        protected void registerStatesAndModels() {
            ModelFile model = models()
                    .cross("glowing_canopy_orchid", modLoc("block/glowing_canopy_orchid"))
                    .renderType("cutout");
            simpleBlock(ModBlocks.GLOWING_CANOPY_ORCHID.get(), model);
        }
    }

    static final class ModItemModels extends ItemModelProvider {
        ModItemModels(PackOutput out, ExistingFileHelper files) { super(out, MODID, files); }

        @Override
        protected void registerModels() {
            singleTexture("canopy_hook_rope", mcLoc("item/handheld"), "layer0", modLoc("item/canopy_hook_rope"));
            singleTexture("glowing_canopy_orchid", mcLoc("item/generated"), "layer0", modLoc("block/glowing_canopy_orchid"));
        }
    }

    static final class ModLang extends LanguageProvider {
        ModLang(PackOutput out) { super(out, MODID, "en_us"); }

        @Override
        protected void addTranslations() {
            add("itemGroup." + MODID, "Echoes of the Canopy");
            addItem(ModItems.CANOPY_HOOK_ROPE, "Canopy Hook-Rope");
            addBlock(ModBlocks.GLOWING_CANOPY_ORCHID, "Glowing Canopy Orchid");
            add("entity." + MODID + ".hook_rope", "Hook-Rope");
        }
    }

    static final class ModRecipes extends RecipeProvider {
        ModRecipes(PackOutput out) { super(out); }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CANOPY_HOOK_ROPE.get())
                    .define('I', Items.IRON_INGOT)
                    .define('V', Items.VINE)
                    .define('S', Items.STRING)
                    .pattern(" I ")
                    .pattern("VSV")
                    .pattern(" S ")
                    .unlockedBy("has_vine", has(Items.VINE))
                    .save(consumer);
        }
    }

    static final class ModBlockLoot extends BlockLootSubProvider {
        ModBlockLoot() { super(Set.of(), FeatureFlags.REGISTRY.allFlags()); }

        @Override
        protected void generate() {
            dropSelf(ModBlocks.GLOWING_CANOPY_ORCHID.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get).collect(Collectors.toList());
        }
    }

    private ModDataGenerators() {}
}
