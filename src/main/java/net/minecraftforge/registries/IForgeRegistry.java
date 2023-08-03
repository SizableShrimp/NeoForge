/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/**
 * Main interface for the registry system. Use this to query the registry system.
 *
 * @param <V> The top level type for the registry
 */
public interface IForgeRegistry<V> extends Iterable<V>
{
    ResourceKey<Registry<V>> getRegistryKey();
    ResourceLocation getRegistryName();

    /**
     * The supplied string key will be prefixed with the currently active mod's mod id.
     * If the supplied name already has a prefix that is different, it will be used and a warning will be logged.
     */
    void register(String key, V value);
    void register(ResourceLocation key, V value);

    boolean containsKey(ResourceLocation key);
    boolean containsValue(V value);
    boolean isEmpty();

    @Nullable V getValue(ResourceLocation key);
    @Nullable ResourceLocation getKey(V value);
    @Nullable ResourceLocation getDefaultKey();
    @NotNull Optional<ResourceKey<V>> getResourceKey(V value);

    @NotNull Set<ResourceLocation>         getKeys();
    @NotNull Collection<V>                 getValues();
    @NotNull Set<Entry<ResourceKey<V>, V>> getEntries();

    /**
     * @see Registry#byNameCodec()
     */
    @NotNull Codec<V> getCodec();

    /**
     * This method exists purely as a stopgap for vanilla compatibility.
     * For anything tag related, use {@link #tags()}.
     *
     * @see Registry#getHolder(ResourceKey)
     */
    @NotNull Optional<Holder<V>> getHolder(ResourceKey<V> key);
    /**
     * This method exists purely as a stopgap for vanilla compatibility.
     * For anything tag related, use {@link #tags()}.
     */
    @NotNull Optional<Holder<V>> getHolder(ResourceLocation location);
    /**
     * This method exists purely as a stopgap for vanilla compatibility.
     * For anything tag related, use {@link #tags()}.
     */
    @NotNull Optional<Holder<V>> getHolder(V value);

    @NotNull Optional<Holder.Reference<V>> getDelegate(ResourceKey<V> rkey);
    @NotNull Holder.Reference<V> getDelegateOrThrow(ResourceKey<V> rkey);
    @NotNull Optional<Holder.Reference<V>> getDelegate(ResourceLocation key);
    @NotNull Holder.Reference<V> getDelegateOrThrow(ResourceLocation key);
    @NotNull Optional<Holder.Reference<V>> getDelegate(V value);
    @NotNull Holder.Reference<V> getDelegateOrThrow(V value);

    /**
     * Retrieve the slave map of type T from the registry.
     * Slave maps are maps which are dependent on registry content in some way.
     * @param slaveMapName The name of the slavemap
     * @param type The type
     * @param <T> Type to return
     * @return The slavemap if present
     */
    <T> T getSlaveMap(ResourceLocation slaveMapName, Class<T> type);

    @FunctionalInterface
    interface MissingFactory<V>
    {
        V createMissing(ResourceLocation key, boolean isNetwork);
    }
}
