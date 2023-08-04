/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.mojang.serialization.Codec;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.StructureModifier;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.holdersets.HolderSetType;

/**
 * A class that exposes static references to all vanilla and Forge registries.
 * Created to have a central place to access the registries directly if modders need.
 * It is still advised that if you are registering things to use {@link RegisterEvent} or {@link net.minecraftforge.registries.DeferredRegister}, but queries and iterations can use this.
 */
public class ForgeRegistries
{
    @SuppressWarnings("unchecked")
    private static <T> Registry<T> getRegistry(ResourceKey<? extends Registry<T>> registryKey) {
        return (Registry<T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
    }

    // Game objects
    public static final Registry<Block> BLOCKS = getRegistry(Registries.BLOCK);
    public static final Registry<Fluid> FLUIDS = getRegistry(Registries.FLUID);
    public static final Registry<Item> ITEMS = getRegistry(Registries.ITEM);
    public static final Registry<MobEffect> MOB_EFFECTS = getRegistry(Registries.MOB_EFFECT);
    public static final Registry<SoundEvent> SOUND_EVENTS = getRegistry(Registries.SOUND_EVENT);
    public static final Registry<Potion> POTIONS = getRegistry(Registries.POTION);
    public static final Registry<Enchantment> ENCHANTMENTS = getRegistry(Registries.ENCHANTMENT);
    public static final Registry<EntityType<?>> ENTITY_TYPES = getRegistry(Registries.ENTITY_TYPE);
    public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = getRegistry(Registries.BLOCK_ENTITY_TYPE);
    public static final Registry<ParticleType<?>> PARTICLE_TYPES = getRegistry(Registries.PARTICLE_TYPE);
    public static final Registry<MenuType<?>> MENU_TYPES = getRegistry(Registries.MENU);
    public static final Registry<PaintingVariant> PAINTING_VARIANTS = getRegistry(Registries.PAINTING_VARIANT);
    public static final Registry<RecipeType<?>> RECIPE_TYPES = getRegistry(Registries.RECIPE_TYPE);
    public static final Registry<RecipeSerializer<?>> RECIPE_SERIALIZERS = getRegistry(Registries.RECIPE_SERIALIZER);
    public static final Registry<Attribute> ATTRIBUTES = getRegistry(Registries.ATTRIBUTE);
    public static final Registry<StatType<?>> STAT_TYPES = getRegistry(Registries.STAT_TYPE);
    public static final Registry<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = getRegistry(Registries.COMMAND_ARGUMENT_TYPE);

    // Villages
    public static final Registry<VillagerProfession> VILLAGER_PROFESSIONS = getRegistry(Registries.VILLAGER_PROFESSION);
    public static final Registry<PoiType> POI_TYPES = getRegistry(Registries.POINT_OF_INTEREST_TYPE);
    public static final Registry<MemoryModuleType<?>> MEMORY_MODULE_TYPES = getRegistry(Registries.MEMORY_MODULE_TYPE);
    public static final Registry<SensorType<?>> SENSOR_TYPES = getRegistry(Registries.SENSOR_TYPE);
    public static final Registry<Schedule> SCHEDULES = getRegistry(Registries.SCHEDULE);
    public static final Registry<Activity> ACTIVITIES = getRegistry(Registries.ACTIVITY);

    // Worldgen
    public static final Registry<WorldCarver<?>> WORLD_CARVERS = getRegistry(Registries.CARVER);
    public static final Registry<Feature<?>> FEATURES = getRegistry(Registries.FEATURE);
    public static final Registry<ChunkStatus> CHUNK_STATUS = getRegistry(Registries.CHUNK_STATUS);
    public static final Registry<BlockStateProviderType<?>> BLOCK_STATE_PROVIDER_TYPES = getRegistry(Registries.BLOCK_STATE_PROVIDER_TYPE);
    public static final Registry<FoliagePlacerType<?>> FOLIAGE_PLACER_TYPES = getRegistry(Registries.FOLIAGE_PLACER_TYPE);
    public static final Registry<TreeDecoratorType<?>> TREE_DECORATOR_TYPES = getRegistry(Registries.TREE_DECORATOR_TYPE);

    // Dynamic/Data driven.
    public static final Registry<Biome> BIOMES = getRegistry(Registries.BIOME);

    // Custom forge registries
    static final DeferredRegister<EntityDataSerializer<?>> DEFERRED_ENTITY_DATA_SERIALIZERS = DeferredRegister.create(Keys.ENTITY_DATA_SERIALIZERS, Keys.ENTITY_DATA_SERIALIZERS.location().getNamespace());
    public static final Registry<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS = DEFERRED_ENTITY_DATA_SERIALIZERS.makeRegistry(registryBuilder -> registryBuilder.sync(true));
    static final DeferredRegister<Codec<? extends IGlobalLootModifier>> DEFERRED_GLOBAL_LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS.location().getNamespace());
    public static final Registry<Codec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS = DEFERRED_GLOBAL_LOOT_MODIFIER_SERIALIZERS.makeRegistry(registryBuilder -> {});
    static final DeferredRegister<Codec<? extends BiomeModifier>> DEFERRED_BIOME_MODIFIER_SERIALIZERS = DeferredRegister.create(Keys.BIOME_MODIFIER_SERIALIZERS, Keys.BIOME_MODIFIER_SERIALIZERS.location().getNamespace());
    public static final Registry<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DEFERRED_BIOME_MODIFIER_SERIALIZERS.makeRegistry(registryBuilder -> {});
    static final DeferredRegister<Codec<? extends StructureModifier>> DEFERRED_STRUCTURE_MODIFIER_SERIALIZERS = DeferredRegister.create(Keys.STRUCTURE_MODIFIER_SERIALIZERS, Keys.STRUCTURE_MODIFIER_SERIALIZERS.location().getNamespace());
    public static final Registry<Codec<? extends StructureModifier>> STRUCTURE_MODIFIER_SERIALIZERS = DEFERRED_STRUCTURE_MODIFIER_SERIALIZERS.makeRegistry(registryBuilder -> {});
    static final DeferredRegister<FluidType> DEFERRED_FLUID_TYPES = DeferredRegister.create(Keys.FLUID_TYPES, Keys.FLUID_TYPES.location().getNamespace());
    public static final Registry<FluidType> FLUID_TYPES = DEFERRED_FLUID_TYPES.makeRegistry(registryBuilder -> {});
    static final DeferredRegister<HolderSetType> DEFERRED_HOLDER_SET_TYPES = DeferredRegister.create(Keys.HOLDER_SET_TYPES, Keys.HOLDER_SET_TYPES.location().getNamespace());
    public static final Registry<HolderSetType> HOLDER_SET_TYPES = DEFERRED_HOLDER_SET_TYPES.makeRegistry(registryBuilder -> {});
    static final DeferredRegister<ItemDisplayContext> DEFERRED_DISPLAY_CONTEXTS = DeferredRegister.create(Keys.DISPLAY_CONTEXTS, Keys.DISPLAY_CONTEXTS.location().getNamespace());
    public static final Registry<ItemDisplayContext> DISPLAY_CONTEXTS = DEFERRED_DISPLAY_CONTEXTS.makeRegistry(registryBuilder -> registryBuilder.sync(true)
            .maxId(128 * 2) // 0 -> 127 gets positive ID, 128 -> 256 gets negative ID
            .defaultKey(new ResourceLocation("none")));

    public static final class Keys {
        // Builtin Registries
        public static final ResourceKey<Registry<EntityDataSerializer<?>>> ENTITY_DATA_SERIALIZERS = key("entity_data_serializers");
        public static final ResourceKey<Registry<Codec<? extends IGlobalLootModifier>>> GLOBAL_LOOT_MODIFIER_SERIALIZERS = key("global_loot_modifier_serializers");
        public static final ResourceKey<Registry<Codec<? extends BiomeModifier>>> BIOME_MODIFIER_SERIALIZERS = key("biome_modifier_serializers");
        public static final ResourceKey<Registry<Codec<? extends StructureModifier>>> STRUCTURE_MODIFIER_SERIALIZERS = key("structure_modifier_serializers");
        public static final ResourceKey<Registry<FluidType>> FLUID_TYPES = key("fluid_type");
        public static final ResourceKey<Registry<HolderSetType>> HOLDER_SET_TYPES = key("holder_set_type");
        public static final ResourceKey<Registry<ItemDisplayContext>> DISPLAY_CONTEXTS = key("display_contexts");

        // Datapack Registries
        public static final ResourceKey<Registry<BiomeModifier>> BIOME_MODIFIERS = key("biome_modifier");
        public static final ResourceKey<Registry<StructureModifier>> STRUCTURE_MODIFIERS = key("structure_modifier");

        private static <T> ResourceKey<Registry<T>> key(String name)
        {
            return ResourceKey.createRegistryKey(new ResourceLocation("forge", name));
        }
    }
}
