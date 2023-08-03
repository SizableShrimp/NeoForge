/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

@ApiStatus.Internal
public class ForgeRegistriesSetup {
    private static boolean setup = false;

    /**
     * Internal forge method. Modders do not call.
     */
    public static void setup(IEventBus modEventBus) {
        synchronized (ForgeRegistriesSetup.class) {
            if (setup)
                throw new IllegalStateException("Setup has already been called!");

            setup = true;
        }

        ForgeRegistries.DEFERRED_ENTITY_DATA_SERIALIZERS.register(modEventBus);
        ForgeRegistries.DEFERRED_GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        ForgeRegistries.DEFERRED_BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        ForgeRegistries.DEFERRED_FLUID_TYPES.register(modEventBus);
        ForgeRegistries.DEFERRED_STRUCTURE_MODIFIER_SERIALIZERS.register(modEventBus);
        ForgeRegistries.DEFERRED_HOLDER_SET_TYPES.register(modEventBus);
        ForgeRegistries.DEFERRED_DISPLAY_CONTEXTS.register(modEventBus);

        modEventBus.addListener(ForgeRegistriesSetup::onModifyRegistry);
    }

    /**
     * The set of vanilla registries which should be serialized to disk.
     */
    private static final Set<ResourceKey<? extends Registry<?>>> VANILLA_SERIALIZE_KEYS = Set.of(
            Registries.MOB_EFFECT, // Required for MobEffectInstance serialization
            Registries.BIOME // Required for chunk Biome paletted containers
    );
    /**
     * The set of vanilla registries which should be synced to the client.
     */
    private static final Set<ResourceKey<? extends Registry<?>>> VANILLA_SYNC_KEYS = Set.of(
            Registries.SOUND_EVENT, // Required for SoundEvent packets
            Registries.MOB_EFFECT, // Required for MobEffect packets
            Registries.BLOCK, // Required for chunk BlockState paletted containers syncing
            Registries.ENCHANTMENT, // Required for EnchantmentMenu syncing
            Registries.ENTITY_TYPE, // Required for Entity spawn packets
            Registries.ITEM, // Required for Item/ItemStack packets
            Registries.PARTICLE_TYPE, // Required for ParticleType packets
            Registries.BLOCK_ENTITY_TYPE, // Required for BlockEntity packets
            Registries.PAINTING_VARIANT, // Required for EntityDataSerializers
            Registries.MENU, // Required for ClientboundOpenScreenPacket
            Registries.COMMAND_ARGUMENT_TYPE, // Required for ClientboundCommandsPacket
            Registries.STAT_TYPE, // Required for ClientboundAwardStatsPacket
            Registries.VILLAGER_TYPE, // Required for EntityDataSerializers
            Registries.VILLAGER_PROFESSION, // Required for EntityDataSerializers
            Registries.CAT_VARIANT, // Required for EntityDataSerializers
            Registries.FROG_VARIANT // Required for EntityDataSerializers
    );

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onModifyRegistry(ModifyRegistryEvent event) {
        if (!(event.getRegistry() instanceof NewForgeRegistry<?> forgeRegistry))
            return;

        if (VANILLA_SERIALIZE_KEYS.contains(event.getRegistryKey()))
            forgeRegistry.setSerialize(true);

        if (VANILLA_SYNC_KEYS.contains(event.getRegistryKey()))
            forgeRegistry.setSync(true);

        if (event.getRegistryKey() == Registries.BLOCK)
            ((NewForgeRegistry) forgeRegistry).addCallback(ForgeRegistryCallbacks.BlockCallbacks.INSTANCE);

        if (event.getRegistryKey() == Registries.ITEM)
            ((NewForgeRegistry) forgeRegistry).addCallback(ForgeRegistryCallbacks.ItemCallbacks.INSTANCE);

        if (event.getRegistryKey() == Registries.ATTRIBUTE)
            ((NewForgeRegistry) forgeRegistry).addCallback(ForgeRegistryCallbacks.AttributeCallbacks.INSTANCE);

        if (event.getRegistryKey() == Registries.POINT_OF_INTEREST_TYPE)
            ((NewForgeRegistry) forgeRegistry).addCallback(ForgeRegistryCallbacks.PoiTypeCallbacks.INSTANCE);
    }
}
