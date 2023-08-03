/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.network.HandshakeMessages;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApiStatus.Internal
public class RegistryManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Marker REGISTRIES = MarkerFactory.getMarker("REGISTRIES");
    private static Set<ResourceLocation> vanillaRegistryKeys = Set.of();
    private static Map<ResourceLocation, RegistrySnapshot> vanillaSnapshot = null;
    private static Map<ResourceLocation, RegistrySnapshot> frozenSnapshot = null;

    public static void postNewRegistryEvent() {
        NewRegistryEvent event = new NewRegistryEvent();
        DataPackRegistryEvent.NewRegistry dataPackEvent = new DataPackRegistryEvent.NewRegistry();
        vanillaRegistryKeys = Set.copyOf(BuiltInRegistries.REGISTRY.keySet());

        ModLoader.get().postEventWrapContainerInModOrder(event);
        ModLoader.get().postEventWrapContainerInModOrder(dataPackEvent);

        event.fill();
        dataPackEvent.process();

        BuiltInRegistries.REGISTRY.forEach(reg -> ModLoader.get().postEventWrapContainerInModOrder(new ModifyRegistryEvent(reg.key(), reg)));
    }

    static void takeVanillaSnapshot() {
        vanillaSnapshot = takeSnapshot(SnapshotType.FULL);
    }

    static void takeFrozenSnapshot() {
        frozenSnapshot = takeSnapshot(SnapshotType.FULL);
    }

    public static void revertToVanilla() {
        applySnapshot(vanillaSnapshot, false);
    }

    public static void revertToFrozen() {
        applySnapshot(frozenSnapshot, false);
    }

    /**
     * Applies the snapshot to the current state of the {@link BuiltInRegistries}.
     *
     * @param snapshots the map of registry name to snapshot
     * @param allowMissing if {@code true}, missing registries will be skipped but will log a warning.
     * Otherwise, an exception will be thrown if a registry name in the snapshot map is missing.
     */
    public static void applySnapshot(Map<ResourceLocation, RegistrySnapshot> snapshots, boolean allowMissing) {
        List<ResourceLocation> missingRegistries = allowMissing ? new ArrayList<>() : null;
        Set<ResourceKey<?>> missingEntries = new HashSet<>();

        snapshots.forEach((registryName, snapshot) -> {
            if (!BuiltInRegistries.REGISTRY.containsKey(registryName)) {
                if (!allowMissing)
                    throw new IllegalStateException("Tried to applied snapshot with registry name " + registryName + " but was not found");

                missingRegistries.add(registryName);
                return;
            }

            MappedRegistry<?> registry = (MappedRegistry<?>) BuiltInRegistries.REGISTRY.get(registryName);
            applySnapshot(registry, snapshot, missingEntries);
        });

        if (missingRegistries != null && !missingRegistries.isEmpty()) {
            String header = "NeoForge detected missing/unknown registries.\n\n" +
                            "There are " + missingRegistries.size() + " missing registries in this save.\n" +
                            "These missing registries will be deleted from the save file on next save.\n" +
                            "Missing Registries:\n";

            StringBuilder builder = new StringBuilder();

            for (ResourceLocation registryName : missingRegistries)
                builder.append(registryName).append("\n");

            LOGGER.warn(REGISTRIES, header);
            LOGGER.warn(REGISTRIES, builder.toString());
        }

        Map<ResourceLocation, Map<ResourceLocation, IdMappingEvent.IdRemapping>> remaps = ImmutableMap.of();

        if (!missingEntries.isEmpty()) {
            remaps = RegistryRemapHandler.handleRemaps(missingEntries);
        }

        GameData.fireRemapEvent(remaps, vanillaSnapshot == snapshots || frozenSnapshot == snapshots);
        ObjectHolderRegistry.applyObjectHolders();
    }

    private static <T> void applySnapshot(MappedRegistry<T> registry, RegistrySnapshot snapshot, Set<ResourceKey<?>> missing) {
        // Needed for package-private operations
        // noinspection UnnecessaryLocalVariable
        NewForgeRegistry<T> forgeRegistry = registry;
        ResourceKey<? extends Registry<T>> registryKey = registry.key();
        Registry<T> backup = snapshot.getFullBackup();

        forgeRegistry.unfreeze();

        if (backup == null) {
            forgeRegistry.clear(false);
            for (var entry : snapshot.getIds().object2IntEntrySet()) {
                ResourceKey<T> key = ResourceKey.create(registryKey, entry.getKey());
                if (!registry.containsKey(key))
                    missing.add(key);
                forgeRegistry.registerIdMapping(key, entry.getIntValue());
            }
        } else {
            forgeRegistry.clear(true);
            for (var entry : backup.entrySet()) {
                ResourceKey<T> key = entry.getKey();
                T value = entry.getValue();
                registry.registerMapping(backup.getId(key), key, value, backup.lifecycle(value));
            }
        }

        snapshot.getAliases().forEach(registry::addAlias);

        forgeRegistry.freeze();
    }

    /**
     * Takes a snapshot of the current registries registered to {@link BuiltInRegistries#REGISTRY}.
     *
     * @param snapshotType If {@link SnapshotType#SAVE_TO_DISK}, only takes a snapshot of registries set to {@linkplain INewForgeRegistry#doesSerialize() serialize}.
     * If {@link SnapshotType#SYNC_TO_CLIENT}, only takes a snapshot of registries set to {@linkplain INewForgeRegistry#doesSync() sync to the client}.
     * If {@link SnapshotType#FULL}, takes a snapshot of all registries including entries.
     * @return the snapshot map of registry name to snapshot data
     */
    public static Map<ResourceLocation, RegistrySnapshot> takeSnapshot(SnapshotType snapshotType) {
        Map<ResourceLocation, RegistrySnapshot> map = new HashMap<>();
        boolean full = snapshotType == SnapshotType.FULL;

        for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
            if (snapshotType == SnapshotType.SAVE_TO_DISK) {
                if (!registry.doesSerialize())
                    continue;
            } else if (snapshotType == SnapshotType.SYNC_TO_CLIENT) {
                if (!registry.doesSync())
                    continue;
            }
            map.put(registry.key().location(), new RegistrySnapshot(registry, full));
        }

        return map;
    }

    public static List<Pair<String, HandshakeMessages.S2CRegistry>> generateRegistryPackets(boolean isLocal) {
        if (isLocal)
            return List.of();

        return takeSnapshot(SnapshotType.SYNC_TO_CLIENT).entrySet().stream()
                .map(e -> Pair.of("Registry " + e.getKey(), new HandshakeMessages.S2CRegistry(e.getKey(), e.getValue())))
                .toList();
    }

    public static List<ResourceLocation> getRegistryNamesForSyncToClient() {
        List<ResourceLocation> list = new ArrayList<>();

        BuiltInRegistries.REGISTRY.entrySet().forEach(e -> {
            if (e.getValue().doesSync())
                list.add(e.getKey().location());
        });

        return list;
    }

    public static Set<ResourceLocation> getVanillaRegistryKeys() {
        return vanillaRegistryKeys;
    }

    public enum SnapshotType {
        /**
         * The snapshot can be saved to disk.
         */
        SAVE_TO_DISK,
        /**
         * The snapshot can be synced to clients.
         */
        SYNC_TO_CLIENT,
        /**
         * A full snapshot is being taken of all registries including entries,
         * never sent to the client or saved to disk.
         */
        FULL
    }
}
