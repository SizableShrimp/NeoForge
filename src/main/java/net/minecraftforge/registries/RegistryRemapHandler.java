/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Set;

@ApiStatus.Internal
class RegistryRemapHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Marker REGISTRIES = MarkerFactory.getMarker("REGISTRIES");

    /**
     * Handles remapping of missing registry entries.
     *
     * @param missing a set of all missing entries based on their resource keys
     * @param isLocalWorld whether the remaps are being handled for a local world or a remote connection
     * @return the set of unhandled missing registry entries after firing remapping events for mods
     */
    static Set<ResourceKey<?>> handleRemaps(Set<ResourceKey<?>> missing, boolean isLocalWorld) {
        LOGGER.debug(REGISTRIES, "There are {} mappings missing", missing.size());

        return Set.copyOf(missing);
        // Multimap<ResourceLocation, ResourceKey<?>> missingGrouped = ArrayListMultimap.create();
        // Multimap<ResourceLocation, ResourceLocation> defaulted = ArrayListMultimap.create();
        // Multimap<ResourceLocation, ResourceLocation> failed = ArrayListMultimap.create();
        //
        // missing.forEach(key -> missingGrouped.put(key.registry(), key));
        //
        // missingGrouped.asMap().forEach((k, v) -> {
        //     MappedRegistry<?> registry = (MappedRegistry<?>) BuiltInRegistries.REGISTRY.get(k);
        //     remapRegistry(defaulted.get(k), failed.get(k), (MappedRegistry) registry, (Collection) v);
        // });
        //
        // if (!defaulted.isEmpty() && !isLocalWorld)
        //     return;
        //
        // if (!defaulted.isEmpty()) {
        //     StringBuilder builder = new StringBuilder("NeoForge detected missing registry entries.\n\n")
        //             .append("There are ").append(defaulted.size()).append(" missing entries in this save.\n")
        //             .append("These missing entries will be deleted from the save file on next save.");
        //
        //     defaulted.asMap().forEach((name, entries) -> {
        //         builder.append("Missing ").append(name).append(":\n");
        //         entries.stream().sorted(ResourceLocation::compareNamespaced)
        //                 .forEach(rl -> builder.append("    ").append(rl).append("\n"));
        //         builder.append("\n");
        //     });
        //
        //     LOGGER.warn(REGISTRIES, builder.toString());
        // }
        //
        // GameData.fireRemapEvent();
    }

    // TODO Reimplement missing mappings event
    // private static <T> void remapRegistry(Collection<ResourceLocation> defaulted, Collection<ResourceLocation> failed, MappedRegistry<T> registry, Collection<ResourceKey<T>> missing) {
    //     Collection<MissingMappingsEvent.Mapping<T>> missingWrapped = missing.stream().map(MissingMappingsEvent.Mapping::new).toList();
    //     MissingMappingsEvent event = new MissingMappingsEvent(registry, missingWrapped);
    //     MinecraftForge.EVENT_BUS.post(event);
    //
    //     List<MissingMappingsEvent.Mapping<T>> mappings = event.getAllMappings(registry.key());
    //     List<MissingMappingsEvent.Mapping<T>> defaultedMappings = mappings.stream()
    //             .filter(e -> e.action == MissingMappingsEvent.Action.DEFAULT)
    //             .toList();
    //
    //     if (!defaultedMappings.isEmpty() && LOGGER.isErrorEnabled(REGISTRIES)) {
    //         StringBuilder sb = new StringBuilder();
    //         sb.append("Unidentified mapping from registry ").append(registry.key().location()).append('\n');
    //         defaultedMappings.forEach(map -> sb.append('\t').append(map.key).append('\n'));
    //         LOGGER.error(REGISTRIES, sb.toString());
    //     }
    //
    //     mappings.forEach(mapping -> {
    //         if (mapping.action == MissingMappingsEvent.Action.DEFAULT) {
    //             defaulted.add(mapping.key.location());
    //         } else if (mapping.action == MissingMappingsEvent.Action.FAIL) {
    //             failed.add(mapping.key.location());
    //         }
    //     });
    //
    //     // processMissing(name, STAGING, event, m.getValue(), remaps.get(name), defaulted.get(name), failed.get(name), !isLocalWorld);
    // }
}
