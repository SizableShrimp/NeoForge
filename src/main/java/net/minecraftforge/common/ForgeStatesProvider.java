/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common;

import java.util.List;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.IModLoadingState;
import net.minecraftforge.fml.IModStateProvider;
import net.minecraftforge.fml.ModLoadingPhase;
import net.minecraftforge.fml.ModLoadingState;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.ObjectHolderRegistry;
import net.minecraftforge.registries.RegistryManager;

public class ForgeStatesProvider implements IModStateProvider
{
    final ModLoadingState CREATE_REGISTRIES = ModLoadingState.withInline("CREATE_REGISTRIES", "CONSTRUCT", ModLoadingPhase.GATHER, ml -> RegistryManager.postNewRegistryEvent());
    final ModLoadingState INJECT_CAPABILITIES = ModLoadingState.withInline("INJECT_CAPABILITIES", "CREATE_REGISTRIES", ModLoadingPhase.GATHER, ml -> CapabilityManager.INSTANCE.injectCapabilities(ml.getAllScanData()));
    final ModLoadingState UNFREEZE = ModLoadingState.withInline("UNFREEZE_DATA", "INJECT_CAPABILITIES", ModLoadingPhase.GATHER, ml -> GameData.unfreezeData());
    final ModLoadingState LOAD_REGISTRIES = ModLoadingState.withInline("LOAD_REGISTRIES", "UNFREEZE_DATA", ModLoadingPhase.GATHER, ml -> GameData.postRegisterEvents());
    final ModLoadingState FREEZE = ModLoadingState.withInline("FREEZE_DATA", "COMPLETE", ModLoadingPhase.COMPLETE, ml -> GameData.freezeData());
    final ModLoadingState NETLOCK = ModLoadingState.withInline("NETWORK_LOCK", "FREEZE_DATA", ModLoadingPhase.COMPLETE, ml -> NetworkRegistry.lock());

    @Override
    public List<IModLoadingState> getAllStates()
    {
        return List.of(CREATE_REGISTRIES, INJECT_CAPABILITIES, UNFREEZE, LOAD_REGISTRIES, FREEZE, NETLOCK);
    }
}
