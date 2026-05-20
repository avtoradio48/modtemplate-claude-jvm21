package com.dbudnik.arboriculturemill.init;

import com.dbudnik.arboriculturemill.Reference;
import com.dbudnik.arboriculturemill.block.BlockArboricultureMill;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class ModBlocks {

    public static final BlockArboricultureMill BLOCK_TEMPLATE = new BlockArboricultureMill();

    @ObjectHolder(Reference.MOD_ID + ":arboriculture_mill")
    public static BlockArboricultureMill ARBORICULTURE_MILL;

    private ModBlocks() {}

    /** Forces the class to load so that the @ObjectHolder field gets resolved early. */
    public static void touch() {}

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(BLOCK_TEMPLATE);
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        ItemBlock itemBlock = new ItemBlock(BLOCK_TEMPLATE);
        itemBlock.setRegistryName(BLOCK_TEMPLATE.getRegistryName());
        event.getRegistry().register(itemBlock);
    }
}
