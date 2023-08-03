/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.debug.misc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(ContainerTypeTest.MODID)
public class ContainerTypeTest
{
    static final String MODID = "containertypetest";
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    public static final RegistryObject<MenuType<TestContainer>> TYPE = MENU_TYPES.register("container",
            () -> IForgeMenuType.create(TestContainer::new));

    public static class TestContainer extends AbstractContainerMenu
    {
        private final String text;

        protected TestContainer(int windowId, Inventory playerInv, FriendlyByteBuf extraData)
        {
            this(windowId, new SimpleContainer(9), extraData.readUtf(128));
        }

        public TestContainer(int windowId, SimpleContainer inv, String text)
        {
            super(TYPE.get(), windowId);
            this.text = text;
            for (int i = 0; i < 9; i++)
            {
                this.addSlot(new Slot(inv, i, (i % 3) * 18, (i / 3) * 18));
            }
        }

        @Override
        public ItemStack quickMoveStack(Player p_38941_, int p_38942_)
        {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player playerIn)
        {
            return true;
        }
    }

    public class TestGui extends AbstractContainerScreen<TestContainer>
    {
        public TestGui(TestContainer container, Inventory inv, Component name)
        {
            super(container, inv, name);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
        {
            graphics.drawString(this.font, getMenu().text, mouseX, mouseY, -1);
        }
    }

    public ContainerTypeTest()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MENU_TYPES.register(modEventBus);
        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::onRightClick);
    }

    private void setup(FMLClientSetupEvent event)
    {
        MenuScreens.register(TYPE.get(), TestGui::new);
    }

    private void onRightClick(PlayerInteractEvent.RightClickBlock event)
    {
        if (!event.getLevel().isClientSide && event.getHand() == InteractionHand.MAIN_HAND)
        {
            if (event.getLevel().getBlockState(event.getPos()).getBlock() == Blocks.SPONGE)
            {
                String text = "Hello World!";
                NetworkHooks.openScreen((ServerPlayer) event.getEntity(), new MenuProvider()
                {
                    @Override
                    public AbstractContainerMenu createMenu(int p_createMenu_1_, Inventory p_createMenu_2_, Player p_createMenu_3_)
                    {
                        SimpleContainer inv = new SimpleContainer(9);
                        for (int i = 0; i < inv.getContainerSize(); i++)
                        {
                            inv.setItem(i, new ItemStack(Items.DIAMOND));
                        }
                        return new TestContainer(p_createMenu_1_, inv, text);
                    }

                    @Override
                    public Component getDisplayName()
                    {
                        return Component.literal("Test");
                    }
                }, extraData -> {
                    extraData.writeUtf(text);
                });
            }
        }
    }
}
