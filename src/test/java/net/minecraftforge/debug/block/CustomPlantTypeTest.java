/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.debug.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod(CustomPlantTypeTest.MODID)
@Mod.EventBusSubscriber(bus = Bus.MOD)
public class CustomPlantTypeTest
{
    static final String MODID = "custom_plant_type_test";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);

    public static final RegistryObject<Block> CUSTOM_SOIL = BLOCKS.register("test_custom_block", CustomBlock::new);
    public static final RegistryObject<Block> CUSTOM_PLANT = BLOCKS.register("test_custom_plant", CustomPlantBlock::new);

    public CustomPlantTypeTest()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
    }

    @SubscribeEvent
    public static void registerItems(RegisterEvent event)
    {
        event.register(Registries.ITEM, helper ->
        {
            helper.register(CUSTOM_SOIL.getId().getPath(), new BlockItem(CUSTOM_SOIL.get(), new Item.Properties()));
            helper.register(CUSTOM_PLANT.getId().getPath(), new BlockItem(CUSTOM_PLANT.get(), new Item.Properties()));
        });
    }

    public static class CustomBlock extends Block
    {
        public CustomBlock()
        {
            super(Properties.of().mapColor(MapColor.STONE));
        }

        @Override
        public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing, IPlantable plantable)
        {
            PlantType type = plantable.getPlantType(level, pos.relative(facing));
            if (type != null && type == CustomPlantBlock.pt)
            {
                return true;
            }
            return super.canSustainPlant(state, level, pos, facing, plantable);
        }
    }

    public static class CustomPlantBlock extends FlowerBlock implements IPlantable
    {
        public static PlantType pt = PlantType.get("custom_plant_type");

        public CustomPlantBlock()
        {
            super(MobEffects.WEAKNESS, 9, Properties.of().mapColor(MapColor.PLANT).noCollission().sound(SoundType.GRASS));
        }

        @Override
        public PlantType getPlantType(BlockGetter level, BlockPos pos)
        {
            return pt;
        }

        @Override
        public BlockState getPlant(BlockGetter level, BlockPos pos)
        {
            return defaultBlockState();
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos)
        {
            BlockState soil = world.getBlockState(pos.below());
            return soil.canSustainPlant(world, pos, Direction.UP, this);
        }

        @Override
        public boolean mayPlaceOn(BlockState state, BlockGetter worldIn, BlockPos pos)
        {
            Block block = state.getBlock();
            return block == CUSTOM_SOIL.get();
        }
    }
}
