/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.minecraft.core.IdMapper;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.CreativeModeTabRegistry;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LogMessageAdapter;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.StartupMessageManager;
import net.minecraftforge.fml.util.EnhancedRuntimeException;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class GameData
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Marker REGISTRIES = MarkerFactory.getMarker("REGISTRIES");

    public static Map<Block, Item> getBlockItemMap()
    {
        return ForgeRegistryCallbacks.ItemCallbacks.BLOCK_TO_ITEM_MAP;
    }

    public static IdMapper<BlockState> getBlockStateIDMap()
    {
        return ForgeRegistryCallbacks.BlockCallbacks.BLOCKSTATE_TO_ID_MAP;
    }

    public static Map<BlockState, PoiType> getBlockStatePointOfInterestTypeMap()
    {
        return ForgeRegistryCallbacks.PoiTypeCallbacks.BLOCKSTATE_TO_POI_TYPE_MAP;
    }

    public static void vanillaSnapshot()
    {
        LOGGER.debug(REGISTRIES, "Creating vanilla freeze snapshot");
        RegistryManager.takeVanillaSnapshot();
        LOGGER.debug(REGISTRIES, "Vanilla freeze snapshot created");
    }

    public static void unfreezeData()
    {
        LOGGER.debug(REGISTRIES, "Unfreezing registries");
        BuiltInRegistries.REGISTRY.stream().filter(r -> r instanceof NewForgeRegistry).forEach(r -> ((NewForgeRegistry<?>) r).unfreeze());
    }

    public static void freezeData()
    {
        LOGGER.debug(REGISTRIES, "Freezing registries");
        BuiltInRegistries.REGISTRY.stream().filter(r -> r instanceof MappedRegistry).forEach(r -> ((MappedRegistry<?>)r).freeze());

        RegistryManager.takeFrozenSnapshot();

        // the id mapping is finalized, no ids actually changed but this is a good place to tell everyone to 'bake' their stuff.
        fireRemapEvent(ImmutableMap.of(), true);

        LOGGER.debug(REGISTRIES, "All registries frozen");
    }

    public static void postRegisterEvents()
    {
        Set<ResourceLocation> ordered = new LinkedHashSet<>(MappedRegistry.getKnownRegistries());
        ordered.retainAll(RegistryManager.getVanillaRegistryKeys());
        ordered.addAll(BuiltInRegistries.REGISTRY.keySet().stream().sorted(ResourceLocation::compareNamespaced).toList());

        RuntimeException aggregate = new RuntimeException();
        for (ResourceLocation rootRegistryName : ordered)
        {
            try
            {
                ResourceKey<? extends Registry<?>> registryKey = ResourceKey.createRegistryKey(rootRegistryName);
                Registry<?> registry = Objects.requireNonNull(BuiltInRegistries.REGISTRY.get(rootRegistryName));
                RegisterEvent registerEvent = new RegisterEvent(registryKey, registry);

                StartupMessageManager.modLoaderConsumer().ifPresent(s -> s.accept("REGISTERING " + registryKey.location()));

                ModLoader.get().postEventWrapContainerInModOrder(registerEvent);

                LOGGER.debug(REGISTRIES, "Applying holder lookups: {}", registryKey.location());
                ObjectHolderRegistry.applyObjectHolders(registryKey.location()::equals);
                LOGGER.debug(REGISTRIES, "Holder lookups applied: {}", registryKey.location());
            } catch (Throwable t)
            {
                aggregate.addSuppressed(t);
            }
        }
        if (aggregate.getSuppressed().length > 0)
        {
            LOGGER.error("Failed to register some entries, see suppressed exceptions for details", aggregate);
            LOGGER.error("Rolling back to VANILLA state");
            RegistryManager.revertToVanilla();
            throw aggregate;
        } else
        {
            ForgeHooks.modifyAttributes();
            SpawnPlacements.fireSpawnPlacementEvent();
            CreativeModeTabRegistry.sortTabs();
        }
    }

    // TODO reg: fix
    // private static <T> void loadRegistry(final ResourceLocation registryName, final RegistryManager from, final RegistryManager to, boolean freeze)
    // {
    //     ForgeRegistry<T> fromRegistry = from.getRegistry(registryName);
    //     if (fromRegistry == null)
    //     {
    //         ForgeRegistry<T> toRegistry = to.getRegistry(registryName);
    //         if (toRegistry == null)
    //         {
    //             throw new EnhancedRuntimeException("Could not find registry to load: " + registryName){
    //                 private static final long serialVersionUID = 1L;
    //                 @Override
    //                 protected void printStackTrace(WrappedPrintStream stream)
    //                 {
    //                     stream.println("Looking For: " + registryName);
    //                     stream.println("Found From:");
    //                     for (ResourceLocation name : from.registries.keySet())
    //                         stream.println("  " + name);
    //                     stream.println("Found To:");
    //                     for (ResourceLocation name : to.registries.keySet())
    //                         stream.println("  " + name);
    //                 }
    //             };
    //         }
    //         // We found it in to, so lets trust to's state...
    //         // This happens when connecting to a server that doesn't have this registry.
    //         // Such as a 1.8.0 Forge server with 1.8.8+ Forge.
    //         // We must however, re-fire the callbacks as some internal data may be corrupted {potions}
    //         //TODO: With my rework of how registries add callbacks are done.. I don't think this is necessary.
    //         //fire addCallback for each entry
    //     }
    //     else
    //     {
    //         ForgeRegistry<T> toRegistry = to.getRegistry(registryName, from);
    //         toRegistry.sync(registryName, fromRegistry);
    //         if (freeze)
    //             toRegistry.isFrozen = true;
    //     }
    // }

    // TODO reg: fix
    // @SuppressWarnings({ "unchecked", "rawtypes" })
    // public static Multimap<ResourceLocation, ResourceLocation> injectSnapshot(Map<ResourceLocation, RegistrySnapshot> snapshot, boolean injectFrozenData, boolean isLocalWorld)
    // {
    //     LOGGER.info(REGISTRIES, "Injecting existing registry data into this {} instance", EffectiveSide.get());
    //     RegistryManager.ACTIVE.registries.forEach((name, reg) -> reg.validateContent(name));
    //     RegistryManager.ACTIVE.registries.forEach((name, reg) -> reg.dump(name));
    //     RegistryManager.ACTIVE.registries.forEach((name, reg) -> reg.resetDelegates());
    //
    //     // Update legacy names
    //     snapshot = snapshot.entrySet().stream()
    //             .sorted(Map.Entry.comparingByKey()) // FIXME Registries need dependency ordering, this makes sure blocks are done before items (for ItemCallbacks) but it's lazy as hell
    //             .collect(Collectors.toMap(e -> RegistryManager.ACTIVE.updateLegacyName(e.getKey()), Map.Entry::getValue, (k1, k2) -> k1, LinkedHashMap::new));
    //
    //     if (isLocalWorld)
    //     {
    //         List<ResourceLocation> missingRegs = snapshot.keySet().stream().filter(name -> !RegistryManager.ACTIVE.registries.containsKey(name)).collect(Collectors.toList());
    //         if (missingRegs.size() > 0)
    //         {
    //             String header = "Forge Mod Loader detected missing/unknown registrie(s).\n\n" +
    //                     "There are " + missingRegs.size() + " missing registries in this save.\n" +
    //                     "If you continue the missing registries will get removed.\n" +
    //                     "This may cause issues, it is advised that you create a world backup before continuing.\n\n";
    //
    //             StringBuilder text = new StringBuilder("Missing Registries:\n");
    //
    //             for (ResourceLocation s : missingRegs)
    //                 text.append(s).append("\n");
    //
    //             LOGGER.warn(REGISTRIES, header);
    //             LOGGER.warn(REGISTRIES, text.toString());
    //         }
    //     }
    //
    //     RegistryManager STAGING = new RegistryManager();
    //
    //     final Map<ResourceLocation, Map<ResourceLocation, IdMappingEvent.IdRemapping>> remaps = Maps.newHashMap();
    //     final LinkedHashMap<ResourceLocation, Map<ResourceLocation, Integer>> missing = Maps.newLinkedHashMap();
    //     // Load the snapshot into the "STAGING" registry
    //     snapshot.forEach((key, value) ->
    //     {
    //         remaps.put(key, Maps.newLinkedHashMap());
    //         missing.put(key, Maps.newLinkedHashMap());
    //         loadPersistentDataToStagingRegistry(RegistryManager.ACTIVE, STAGING, remaps.get(key), missing.get(key), key, value);
    //     });
    //
    //     int count = missing.values().stream().mapToInt(Map::size).sum();
    //     if (count > 0)
    //     {
    //         LOGGER.debug(REGISTRIES,"There are {} mappings missing - attempting a mod remap", count);
    //         Multimap<ResourceLocation, ResourceLocation> defaulted = ArrayListMultimap.create();
    //         Multimap<ResourceLocation, ResourceLocation> failed = ArrayListMultimap.create();
    //
    //         missing.entrySet().stream().filter(e -> e.getValue().size() > 0).forEach(m ->
    //         {
    //             ResourceLocation name = m.getKey();
    //             ForgeRegistry<?> reg = STAGING.getRegistry(name);
    //             MissingMappingsEvent event = reg.getMissingEvent(name, m.getValue());
    //             MinecraftForge.EVENT_BUS.post(event);
    //
    //             List<MissingMappingsEvent.Mapping<?>> lst = event.getAllMappings(reg.getRegistryKey()).stream()
    //                     .filter(e -> e.action == MissingMappingsEvent.Action.DEFAULT)
    //                     .sorted(Comparator.comparing(Object::toString))
    //                     .collect(Collectors.toList());
    //             if (!lst.isEmpty())
    //             {
    //                 LOGGER.error(REGISTRIES, () -> LogMessageAdapter.adapt(sb -> {
    //                    sb.append("Unidentified mapping from registry ").append(name).append('\n');
    //                    lst.stream().sorted().forEach(map -> sb.append('\t').append(map.key).append(": ").append(map.id).append('\n'));
    //                 }));
    //             }
    //             event.getAllMappings(reg.getRegistryKey()).stream()
    //                     .filter(e -> e.action == MissingMappingsEvent.Action.FAIL)
    //                     .forEach(fail -> failed.put(name, fail.key));
    //
    //             processMissing(name, STAGING, event, m.getValue(), remaps.get(name), defaulted.get(name), failed.get(name), !isLocalWorld);
    //         });
    //
    //         if (!defaulted.isEmpty() && !isLocalWorld)
    //             return defaulted;
    //
    //         if (!defaulted.isEmpty())
    //         {
    //             String header = "Forge Mod Loader detected missing registry entries.\n\n" +
    //                "There are " + defaulted.size() + " missing entries in this save.\n" +
    //                "If you continue the missing entries will get removed.\n" +
    //                "A world backup will be automatically created in your saves directory.\n\n";
    //
    //             StringBuilder buf = new StringBuilder();
    //             defaulted.asMap().forEach((name, entries) ->
    //             {
    //                 buf.append("Missing ").append(name).append(":\n");
    //                 entries.stream().sorted((o1, o2) -> o1.compareNamespaced(o2)).forEach(rl -> buf.append("    ").append(rl).append("\n"));
    //                 buf.append("\n");
    //             });
    //
    //             LOGGER.warn(REGISTRIES, header);
    //             LOGGER.warn(REGISTRIES, buf.toString());
    //         }
    //
    //         if (!defaulted.isEmpty())
    //         {
    //             if (isLocalWorld)
    //                 LOGGER.error(REGISTRIES, "There are unidentified mappings in this world - we are going to attempt to process anyway");
    //         }
    //
    //     }
    //
    //     if (injectFrozenData)
    //     {
    //         // If we're loading up the world from disk, we want to add in the new data that might have been provisioned by mods
    //         // So we load it from the frozen persistent registry
    //         RegistryManager.ACTIVE.registries.forEach((name, reg) ->
    //         {
    //             loadFrozenDataToStagingRegistry(STAGING, name, remaps.get(name));
    //         });
    //     }
    //
    //     // Validate that all the STAGING data is good
    //     STAGING.registries.forEach((name, reg) -> reg.validateContent(name));
    //
    //     // Load the STAGING registry into the ACTIVE registry
    //     //for (Map.Entry<ResourceLocation, IForgeRegistry<?>>> r : RegistryManager.ACTIVE.registries.entrySet())
    //     RegistryManager.ACTIVE.registries.forEach((key, value) ->
    //     {
    //         loadRegistry(key, STAGING, RegistryManager.ACTIVE, true);
    //     });
    //
    //     RegistryManager.ACTIVE.registries.forEach((name, reg) -> {
    //         reg.bake();
    //
    //         // Dump the active registry
    //         reg.dump(name);
    //     });
    //
    //     // Tell mods that the ids have changed
    //     fireRemapEvent(remaps, false);
    //
    //     // The id map changed, ensure we apply object holders
    //     ObjectHolderRegistry.applyObjectHolders();
    //
    //     // Return an empty list, because we're good
    //     return ArrayListMultimap.create();
    // }

    static void fireRemapEvent(final Map<ResourceLocation, Map<ResourceLocation, IdMappingEvent.IdRemapping>> remaps, final boolean isFreezing) {
        MinecraftForge.EVENT_BUS.post(new IdMappingEvent(remaps, isFreezing));
    }

    // TODO reg: fix
    // //Has to be split because of generics, Yay!
    // private static <T> void loadPersistentDataToStagingRegistry(RegistryManager pool, RegistryManager to, Map<ResourceLocation, IdMappingEvent.IdRemapping> remaps, Map<ResourceLocation, Integer> missing, ResourceLocation name, RegistrySnapshot snap)
    // {
    //     ForgeRegistry<T> active  = pool.getRegistry(name);
    //     if (active == null)
    //         return; // We've already asked the user if they wish to continue. So if the reg isnt found just assume the user knows and accepted it.
    //     ForgeRegistry<T> _new = to.getRegistry(name, RegistryManager.ACTIVE);
    //     snap.aliases.forEach(_new::addAlias);
    //     snap.blocked.forEach(_new::block);
    //     _new.loadIds(snap.ids, snap.overrides, missing, remaps, active, name);
    // }
    //
    // //Another bouncer for generic reasons
    // @SuppressWarnings("unchecked")
    // private static <T> void processMissing(ResourceLocation name, RegistryManager STAGING, MissingMappingsEvent e, Map<ResourceLocation, Integer> missing, Map<ResourceLocation, IdMappingEvent.IdRemapping> remaps, Collection<ResourceLocation> defaulted, Collection<ResourceLocation> failed, boolean injectNetworkDummies)
    // {
    //     List<MissingMappingsEvent.Mapping<T>> mappings = e.getAllMappings(ResourceKey.createRegistryKey(name));
    //     ForgeRegistry<T> active = RegistryManager.ACTIVE.getRegistry(name);
    //     ForgeRegistry<T> staging = STAGING.getRegistry(name);
    //     staging.processMissingEvent(name, active, mappings, missing, remaps, defaulted, failed, injectNetworkDummies);
    // }
    //
    // private static <T> void loadFrozenDataToStagingRegistry(RegistryManager STAGING, ResourceLocation name, Map<ResourceLocation, IdMappingEvent.IdRemapping> remaps)
    // {
    //     ForgeRegistry<T> frozen = RegistryManager.FROZEN.getRegistry(name);
    //     ForgeRegistry<T> newRegistry = STAGING.getRegistry(name, RegistryManager.FROZEN);
    //     Map<ResourceLocation, Integer> _new = Maps.newLinkedHashMap();
    //     frozen.getKeys().stream().filter(key -> !newRegistry.containsKey(key)).forEach(key -> _new.put(key, frozen.getID(key)));
    //     newRegistry.loadIds(_new, frozen.getOverrideOwners(), Maps.newLinkedHashMap(), remaps, frozen, name);
    // }
}
