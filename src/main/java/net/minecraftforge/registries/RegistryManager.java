/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.mojang.logging.LogUtils;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.network.HandshakeMessages;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class RegistryManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Marker REGISTRIES = MarkerFactory.getMarker("REGISTRIES");
    private static Set<ResourceLocation> vanillaRegistryKeys = Set.of();
    private static Map<ResourceLocation, RegistrySnapshot> vanillaSnapshot = null;
    private static Map<ResourceLocation, RegistrySnapshot> frozenSnapshot = null;
    private static Map<ResourceLocation, RegistrySnapshot> currentDiskSnapshot = null;

    public static void postNewRegistryEvent() {
        NewRegistryEvent event = new NewRegistryEvent();
        DataPackRegistryEvent.NewRegistry dataPackEvent = new DataPackRegistryEvent.NewRegistry();
        vanillaRegistryKeys = Set.copyOf(BuiltInRegistries.REGISTRY.keySet());

        ModLoader.get().postEventWrapContainerInModOrder(event);
        ModLoader.get().postEventWrapContainerInModOrder(dataPackEvent);

        event.fill();
        dataPackEvent.process();

        BuiltInRegistries.REGISTRY.forEach(RegistryManager::postModifyRegistryEvent);
    }

    public static void postModifyRegistryEvent(Registry<?> registry) {
        ModLoader.get().postEventWrapContainerInModOrder(new ModifyRegistryEvent(registry));
    }

    static void takeVanillaSnapshot() {
        vanillaSnapshot = takeSnapshot(null, SnapshotType.FULL);
    }

    static void takeFrozenSnapshot() {
        frozenSnapshot = takeSnapshot(null, SnapshotType.FULL);
    }

    public static void setCurrentDiskSnapshot(Map<ResourceLocation, RegistrySnapshot> snapshot) {
        currentDiskSnapshot = snapshot;
    }

    public static void applyCurrentDiskSnapshot(RegistryAccess registryAccess) {
        // If it's null then someone is incorrectly calling this method or this is a new save
        if (currentDiskSnapshot == null)
            return;

        Set<ResourceKey<?>> failedElements = RegistryManager.applySnapshot(registryAccess, currentDiskSnapshot, true, true);
        currentDiskSnapshot = null;

        if (!failedElements.isEmpty() && LOGGER.isErrorEnabled()) {
            StringBuilder buf = new StringBuilder()
                    .append("There are ").append(failedElements.size()).append(" unassigned registry entries in this save.\n\n");

            failedElements.forEach(k -> buf.append("Missing ").append(k).append('\n'));

            LOGGER.error(buf.toString());
        }
    }

    public static void revertToVanilla() {
        applySnapshot(null, vanillaSnapshot, false, true);
    }

    public static void revertToFrozen() {
        applySnapshot(null, frozenSnapshot, false, true);
    }

    /**
     * Applies the snapshot to the current state of the provided registry access,
     * or {@link BuiltInRegistries#REGISTRY} if the registry access is null.
     *
     * @param registryAccess the registry access to apply the snapshot to, or {@code null} to use {@link BuiltInRegistries#REGISTRY}
     * @param snapshots the map of registry name to snapshot
     * @param allowMissing if {@code true}, missing registries will be skipped but will log a warning.
     * Otherwise, an exception will be thrown if a registry name in the snapshot map is missing.
     * @param isLocalWorld changes the logging depending on if the snapshot is coming from a local save or a remote connection
     * @return the set of unhandled missing registry entries after firing remapping events for mods
     */
    public static Set<ResourceKey<?>> applySnapshot(@Nullable RegistryAccess registryAccess, Map<ResourceLocation, RegistrySnapshot> snapshots, boolean allowMissing, boolean isLocalWorld) {
        List<ResourceLocation> missingRegistries = allowMissing ? new ArrayList<>() : null;
        Set<ResourceKey<?>> missingEntries = new HashSet<>();
        Set<ResourceLocation> registryKeys = registryAccess == null
                ? BuiltInRegistries.REGISTRY.keySet()
                : registryAccess.registries().map(e -> e.key().location()).collect(Collectors.toSet());

        snapshots.forEach((registryName, snapshot) -> {
            if (!registryKeys.contains(registryName)) {
                if (!allowMissing)
                    throw new IllegalStateException("Tried to applied snapshot with registry name " + registryName + " but was not found");

                missingRegistries.add(registryName);
                return;
            }

            MappedRegistry<?> registry = (MappedRegistry<?>) (registryAccess == null
                    ? BuiltInRegistries.REGISTRY.get(registryName)
                    : registryAccess.registryOrThrow(ResourceKey.createRegistryKey(registryName)));
            applySnapshot(registry, snapshot, missingEntries);
        });

        if (missingRegistries != null && !missingRegistries.isEmpty() && LOGGER.isWarnEnabled(REGISTRIES)) {
            StringBuilder builder = new StringBuilder("NeoForge detected missing/unknown registries.\n\n")
                    .append("There are ").append(missingRegistries.size()).append(" missing registries.\n");
            if (isLocalWorld)
                builder.append("These missing registries will be deleted from the save file on next save.\n");

            builder.append("Missing Registries:\n");

            for (ResourceLocation registryName : missingRegistries)
                builder.append(registryName).append("\n");

            LOGGER.warn(REGISTRIES, builder.toString());
        }

        Set<ResourceKey<?>> unhandled = missingEntries.isEmpty()
                ? Set.of()
                : RegistryRemapHandler.handleRemaps(missingEntries, isLocalWorld);

        ObjectHolderRegistry.applyObjectHolders();

        return unhandled;
    }

    private static <T> void applySnapshot(MappedRegistry<T> registry, RegistrySnapshot snapshot, Set<ResourceKey<?>> missing) {
        // Needed for package-private operations
        // noinspection UnnecessaryLocalVariable
        ForgeRegistry<T> forgeRegistry = registry;
        ResourceKey<? extends Registry<T>> registryKey = registry.key();
        Registry<T> backup = snapshot.getFullBackup();

        forgeRegistry.unfreeze();

        if (backup == null) {
            forgeRegistry.clear(false);
            for (var entry : snapshot.getIds().object2IntEntrySet()) {
                ResourceKey<T> key = ResourceKey.create(registryKey, entry.getKey());
                if (!registry.containsKey(key)) {
                    missing.add(key);
                } else {
                    forgeRegistry.registerIdMapping(key, entry.getIntValue());
                }
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
     * Takes a snapshot of the current registries registered to the provided registry access,
     * or {@link BuiltInRegistries#REGISTRY} if the registry access is null.
     *
     * @param registryAccess the registry access to take a snapshot of, or {@code null} to use {@link BuiltInRegistries#REGISTRY}
     * @param snapshotType If {@link SnapshotType#SAVE_TO_DISK}, only takes a snapshot of registries set to {@linkplain IForgeRegistry#doesSerialize() serialize}.
     * If {@link SnapshotType#SYNC_TO_CLIENT}, only takes a snapshot of registries set to {@linkplain IForgeRegistry#doesSync() sync to the client}.
     * If {@link SnapshotType#FULL}, takes a snapshot of all registries including entries.
     * @return the snapshot map of registry name to snapshot data
     */
    public static Map<ResourceLocation, RegistrySnapshot> takeSnapshot(@Nullable RegistryAccess registryAccess, SnapshotType snapshotType) {
        Map<ResourceLocation, RegistrySnapshot> map = new HashMap<>();
        boolean full = snapshotType == SnapshotType.FULL;
        Iterable<? extends Registry<?>> registries = registryAccess == null
                ? BuiltInRegistries.REGISTRY
                : () -> registryAccess.registries().<Registry<?>>map(RegistryAccess.RegistryEntry::value).iterator();

        for (Registry<?> registry : registries) {
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

        // Datapack registries have their own dedicated syncing process, so we pass in null for the registry access
        return takeSnapshot(null, SnapshotType.SYNC_TO_CLIENT).entrySet().stream()
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
