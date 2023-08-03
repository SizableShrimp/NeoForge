/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.Event;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.List;

/**
 * Fired on the {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS forge bus}.
 */
public class MissingMappingsEvent extends Event
{
    private final ResourceKey<? extends Registry<?>> key;
    private final Registry<?> registry;
    private final List<Mapping<?>> mappings;

    @ApiStatus.Internal
    public <T> MissingMappingsEvent(Registry<T> registry, Collection<Mapping<T>> missed)
    {
        this.key = registry.key();
        this.registry = registry;
        this.mappings = List.copyOf(missed);
    }

    public ResourceKey<? extends Registry<?>> getKey()
    {
        return this.key;
    }

    public Registry<?> getRegistry()
    {
        return this.registry;
    }

    /**
     * @return An immutable list of missing mappings for the given namespace.
     * Empty if the registry key doesn't match {@link #getKey()}.
     */
    @SuppressWarnings("unchecked")
    public <T> List<Mapping<T>> getMappings(ResourceKey<? extends Registry<T>> registryKey, String namespace)
    {
        return registryKey == this.key
                ? (List<Mapping<T>>) (List<?>) this.mappings.stream().filter(e -> e.key.location().getNamespace().equals(namespace)).toList()
                : List.of();
    }

    /**
     * @return An immutable list of all missing mappings.
     * Empty if the registry key doesn't match {@link #getKey()}.
     */
    @SuppressWarnings("unchecked")
    public <T> List<Mapping<T>> getAllMappings(ResourceKey<? extends Registry<T>> registryKey)
    {
        return registryKey == this.key ? (List<Mapping<T>>) (List<?>) this.mappings : List.of();
    }

    /**
     * Actions you can take with this missing mapping.
     * <ul>
     * <li>{@link #IGNORE} means this missing mapping will be ignored.
     * <li>{@link #WARN} means this missing mapping will generate a warning.
     * <li>{@link #FAIL} means this missing mapping will prevent the world from loading.
     * </ul>
     */
    public enum Action
    {
        /**
         * Take the default action
         */
        DEFAULT,
        /**
         * Ignore this missing mapping. This means the mapping will be abandoned
         */
        IGNORE,
        /**
         * Generate a warning but allow loading to continue
         */
        WARN,
        /**
         * Fail to load
         */
        FAIL,
        /**
         * Remap this name to a new name (add a migration mapping)
         */
        REMAP
    }

    public static class Mapping<T> implements Comparable<Mapping<T>>
    {
        final ResourceKey<T> key;
        Action action = Action.DEFAULT;
        T target;

        @ApiStatus.Internal
        public Mapping(ResourceKey<T> key)
        {
            this.key = key;
        }

        /**
         * Ignore the missing item.
         */
        public void ignore()
        {
            action = Action.IGNORE;
        }

        /**
         * Warn the user about the missing item.
         */
        public void warn()
        {
            action = Action.WARN;
        }

        /**
         * Prevent the world from loading due to the missing item.
         */
        public void fail()
        {
            action = Action.FAIL;
        }

        /**
         * Remap the missing entry to the specified object.
         * <p>
         * Use this if you have renamed an entry.
         * Existing references using the old name will point to the new one.
         *
         * @param target Entry to remap to.
         */
        public void remap(T target)
        {
            Validate.notNull(target, "Remap target can not be null");
            action = Action.REMAP;
            this.target = target;
        }

        public ResourceKey<T> getKey()
        {
            return key;
        }

        @Override
        public int compareTo(Mapping<T> o)
        {
            return this.key.compareTo(o.key);
        }
    }
}
