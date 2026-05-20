package com.dbudnik.arboriculturemill.proxy;

import com.dbudnik.arboriculturemill.Reference;
import com.dbudnik.arboriculturemill.init.ModBlocks;
import com.dbudnik.arboriculturemill.tile.TileEntityArboricultureMill;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CommonProxy implements IProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(
                TileEntityArboricultureMill.class,
                new net.minecraft.util.ResourceLocation(Reference.MOD_ID, "arboriculture_mill")
        );
        ModBlocks.touch();
    }
}
