package com.dbudnik.arboriculturemill.client;

import com.dbudnik.arboriculturemill.Reference;
import com.dbudnik.arboriculturemill.init.ModBlocks;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Registers item models on the client.
 *
 * {@code ModelRegistryEvent} is the canonical hook for
 * {@link ModelLoader#setCustomModelResourceLocation} in 1.12.2 — registering
 * here removes any doubt about lifecycle timing. The {@code Side.CLIENT} guard
 * keeps this class (and its client-only imports) off the dedicated server.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Reference.MOD_ID)
public final class ClientModelRegistration {

    private ClientModelRegistration() {}

    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        registerItemModel(Item.getItemFromBlock(ModBlocks.ARBORICULTURE_MILL));
    }

    private static void registerItemModel(Item item) {
        if (item == null || item.getRegistryName() == null) {
            return;
        }
        ModelLoader.setCustomModelResourceLocation(
                item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
