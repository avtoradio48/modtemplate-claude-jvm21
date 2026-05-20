package com.dbudnik.arboriculturemill.init;

import com.dbudnik.arboriculturemill.Reference;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public final class ModCreativeTab extends CreativeTabs {

    public static final ModCreativeTab INSTANCE = new ModCreativeTab();

    private ModCreativeTab() {
        super(Reference.MOD_ID);
    }

    @Override
    public ItemStack createIcon() {
        return ModBlocks.ARBORICULTURE_MILL == null
                ? ItemStack.EMPTY
                : new ItemStack(ModBlocks.ARBORICULTURE_MILL);
    }
}
