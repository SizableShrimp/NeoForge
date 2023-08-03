/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RegistryRemapHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Marker REGISTRIES = MarkerFactory.getMarker("REGISTRIES");

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Map<ResourceLocation, Map<ResourceLocation, IdMappingEvent.IdRemapping>> handleRemaps(Set<ResourceKey<?>> missing) {
        LOGGER.debug(REGISTRIES, "There are {} mappings missing - attempting a mod remap", missing.size());
        Multimap<ResourceLocation, ResourceKey<?>> missingGrouped = ArrayListMultimap.create();
        Multimap<ResourceLocation, ResourceLocation> defaulted = ArrayListMultimap.create();
        Multimap<ResourceLocation, ResourceLocation> failed = ArrayListMultimap.create();

        missing.forEach(key -> missingGrouped.put(key.registry(), key));

        missingGrouped.asMap().forEach((k, v) -> {
            MappedRegistry<?> registry = (MappedRegistry<?>) BuiltInRegistries.REGISTRY.get(k);
            remapRegistry(defaulted.get(k), failed.get(k), (MappedRegistry) registry, (Collection) v);
        });

        // TODO reg: change logging if we are on a remote server connection
        // if (!defaulted.isEmpty() && !allowMissing)
        //     return defaulted;

        if (!defaulted.isEmpty()) {
            String header = "NeoForge detected missing registry entries.\n\n" +
                            "There are " + defaulted.size() + " missing entries in this save.\n" +
                            "These missing entries will be deleted from the save file on next save.";

            StringBuilder buf = new StringBuilder();
            defaulted.asMap().forEach((name, entries) -> {
                buf.append("Missing ").append(name).append(":\n");
                entries.stream().sorted(ResourceLocation::compareNamespaced).forEach(rl -> buf.append("    ").append(rl).append("\n"));
                buf.append("\n");
            });

            LOGGER.warn(REGISTRIES, header);
            LOGGER.warn(REGISTRIES, buf.toString());
        }

        // TODO reg: fix
        return Map.of();
    }

    private static <T> void remapRegistry(Collection<ResourceLocation> defaulted, Collection<ResourceLocation> failed, MappedRegistry<T> registry, Collection<ResourceKey<T>> missing) {
        Collection<MissingMappingsEvent.Mapping<T>> missingWrapped = missing.stream().map(MissingMappingsEvent.Mapping::new).toList();
        MissingMappingsEvent event = new MissingMappingsEvent(registry, missingWrapped);
        MinecraftForge.EVENT_BUS.post(event);

        List<MissingMappingsEvent.Mapping<T>> mappings = event.getAllMappings(registry.key());
        List<MissingMappingsEvent.Mapping<T>> defaultedMappings = mappings.stream()
                .filter(e -> e.action == MissingMappingsEvent.Action.DEFAULT)
                .toList();

        if (!defaultedMappings.isEmpty() && LOGGER.isErrorEnabled(REGISTRIES)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unidentified mapping from registry ").append(registry.key().location()).append('\n');
            defaultedMappings.forEach(map -> sb.append('\t').append(map.key).append('\n'));
            LOGGER.error(REGISTRIES, sb.toString());
        }

        mappings.forEach(mapping -> {
            if (mapping.action == MissingMappingsEvent.Action.DEFAULT) {
                defaulted.add(mapping.key.location());
            } else if (mapping.action == MissingMappingsEvent.Action.FAIL) {
                failed.add(mapping.key.location());
            }
        });

        // TODO reg: fix
        // processMissing(name, STAGING, event, m.getValue(), remaps.get(name), defaulted.get(name), failed.get(name), !isLocalWorld);
    }
}
