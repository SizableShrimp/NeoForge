/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.debug.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod(SlipperinessTest.MOD_ID)
@EventBusSubscriber
public class SlipperinessTest
{
    static final String MOD_ID = "slipperiness_test";
    static final String BLOCK_ID = "test_block";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);

    public static final RegistryObject<Block> BB_BLOCK = BLOCKS.register(BLOCK_ID, () -> new Block(Block.Properties.of())
        {
            @Override
            public float getFriction(BlockState state, LevelReader level, BlockPos pos, Entity entity)
            {
                return entity instanceof Boat ? 2 : super.getFriction(state, level, pos, entity);
            }
        });

    public SlipperinessTest() {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    @SubscribeEvent
    public static void registerItems(RegisterEvent e)
    {
        e.register(Registries.ITEM, helper -> helper.register(BLOCK_ID, new BlockItem(BB_BLOCK.get(), new Item.Properties())));
    }

    /*
    @EventBusSubscriber(value = Dist.CLIENT, modid = MOD_ID)
    public static class ClientEventHandler
    {
        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event)
        {
            ModelLoader.setCustomStateMapper(BB_BLOCK, block -> Collections.emptyMap());
            ModelBakery.registerItemVariants(Item.getItemFromBlock(BB_BLOCK));
        }
    }
    */
}
