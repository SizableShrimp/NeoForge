/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired after all builtin registries are constructed with a specific registry to be able to modify.
 * This event is intended to be used to register callbacks for a specific registry.
 * It is fired for all registries, including registries in {@link BuiltInRegistries}.
 *
 * <p>This event is not {@linkplain Cancelable cancellable}, and does not {@linkplain HasResult have a result}.</p>
 * <p>This event is fired on the {@linkplain FMLJavaModLoadingContext#getModEventBus() mod-specific event bus},
 * on both {@linkplain LogicalSide logical sides}.</p>
 *
 * @see NewRegistryEvent
 */
public class ModifyRegistryEvent extends Event implements IModBusEvent {
    private final ResourceKey<? extends Registry<?>> registryKey;
    private final Registry<?> registry;

    @ApiStatus.Internal
    public ModifyRegistryEvent(ResourceKey<? extends Registry<?>> registryKey, Registry<?> registry) {
        this.registryKey = registryKey;
        this.registry = registry;
    }

    public ResourceKey<? extends Registry<?>> getRegistryKey() {
        return this.registryKey;
    }

    public Registry<?> getRegistry() {
        return this.registry;
    }
}
