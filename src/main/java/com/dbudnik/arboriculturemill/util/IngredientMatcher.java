package com.dbudnik.arboriculturemill.util;

import com.dbudnik.arboriculturemill.Reference;
import com.dbudnik.arboriculturemill.integration.TreeRecipeLookup;
import forestry.api.arboriculture.ITree;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;

public final class IngredientMatcher {

    private static final ResourceLocation FORESTRY_FERTILIZER =
            new ResourceLocation(Reference.FORESTRY_MOD_ID, "fertilizer_compound");

    private IngredientMatcher() {}

    public static boolean isSapling(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ITree tree = TreeRecipeLookup.asTree(stack);
        return tree != null;
    }

    /**
     * Accepts Forestry mineral fertilizer. We also accept any item registered
     * under the OreDict name "fertilizer" so that addon fertilizers slot in.
     */
    public static boolean isFertilizer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation regName = stack.getItem().getRegistryName();
        if (FORESTRY_FERTILIZER.equals(regName)) return true;
        int[] oreIds = OreDictionary.getOreIDs(stack);
        for (int id : oreIds) {
            String name = OreDictionary.getOreName(id);
            if ("fertilizer".equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    @Nullable
    public static Fluid waterFluid() {
        return FluidRegistry.WATER;
    }
}
