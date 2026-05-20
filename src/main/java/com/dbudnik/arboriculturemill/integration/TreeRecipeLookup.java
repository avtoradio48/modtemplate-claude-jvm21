package com.dbudnik.arboriculturemill.integration;

import com.dbudnik.arboriculturemill.ArboricultureMill;
import forestry.api.arboriculture.EnumGermlingType;
import forestry.api.arboriculture.EnumTreeChromosome;
import forestry.api.arboriculture.IAlleleTreeSpecies;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.ITreeRoot;
import forestry.api.arboriculture.IWoodProvider;
import forestry.api.arboriculture.TreeManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleFloat;
import forestry.api.genetics.IAlleleInteger;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IGenome;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * Bridges raw Forestry sapling ItemStacks to the wood / fruit drops that the
 * mill should emit. All numbers come from the genome: the species defines the
 * base product list, while yield / maturation / sappiness alleles modulate
 * count, processing time, and the chance of bonus saplings.
 *
 * Forestry's wood/fruit provider methods have shifted names across versions
 * (getWoodStack vs getLogStack(int), getProducts vs produceStacks(...)). To
 * keep this single source file compatible with 5.8.x without taking a hard
 * dependency on a specific method, we use reflective probes for the two
 * methods that historically vary.
 */
public final class TreeRecipeLookup {

    /** Base ticks per operation when the maturation allele speed is 1.0. */
    public static final int BASE_TICKS_PER_OPERATION = 200;
    /** Minimum ticks per operation, no matter how fast maturation is. */
    public static final int MIN_TICKS_PER_OPERATION = 40;

    /** Base products multiplier — gets multiplied by yield speed for count. */
    public static final int BASE_PRODUCTS_PER_OPERATION = 4;
    /** Maximum stack count we want to emit per output slot in one operation. */
    public static final int MAX_PRODUCTS_PER_OPERATION = 32;

    private TreeRecipeLookup() {}

    @Nullable
    public static ITree asTree(ItemStack saplingStack) {
        ITreeRoot root = treeRoot();
        if (root == null || saplingStack.isEmpty()) return null;
        try {
            return root.getMember(saplingStack);
        } catch (Throwable t) {
            ArboricultureMill.LOGGER.debug("TreeRoot#getMember failed", t);
            return null;
        }
    }

    @Nullable
    public static ITreeRoot treeRoot() {
        try {
            return TreeManager.treeRoot;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Compute output items for one finished operation. World and pos are used
     * by Forestry's fruit provider to determine the ripening drops.
     */
    public static NonNullList<ItemStack> rollProducts(ITree tree, World world, BlockPos pos, Random rand) {
        NonNullList<ItemStack> result = NonNullList.create();
        if (tree == null) return result;

        IGenome genome = tree.getGenome();
        IAlleleSpecies primary = genome.getPrimary();
        if (!(primary instanceof IAlleleTreeSpecies)) return result;
        IAlleleTreeSpecies species = (IAlleleTreeSpecies) primary;

        float yieldSpeed = readFloatAllele(genome, EnumTreeChromosome.YIELD, 1.0F);
        // Yield is roughly in [0.1, 1.5]; map to a sensible mill multiplier.
        float multiplier = Math.max(0.25F, Math.min(yieldSpeed * 2.0F, 4.0F));

        ItemStack wood = readWood(species);
        if (!wood.isEmpty()) {
            ItemStack scaledWood = wood.copy();
            int amount = Math.max(1, Math.min(MAX_PRODUCTS_PER_OPERATION,
                    Math.round(BASE_PRODUCTS_PER_OPERATION * multiplier)));
            scaledWood.setCount(amount);
            result.add(scaledWood);
        }

        NonNullList<ItemStack> fruits = readFruits(tree, world, pos);
        for (ItemStack fruit : fruits) {
            if (fruit.isEmpty()) continue;
            ItemStack scaledFruit = fruit.copy();
            int amount = Math.max(1, Math.min(MAX_PRODUCTS_PER_OPERATION,
                    Math.round(fruit.getCount() * multiplier) + (rand.nextInt(3) - 1)));
            scaledFruit.setCount(Math.max(1, amount));
            result.add(scaledFruit);
        }
        return result;
    }

    /** Bonus saplings from sappiness — emitted only sometimes. */
    public static ItemStack rollSaplingBonus(ITree tree, Random rand) {
        if (tree == null) return ItemStack.EMPTY;
        float sappiness = readFloatAllele(tree.getGenome(), EnumTreeChromosome.SAPPINESS, 0.0F);
        // Sappiness ~ [0.1, 1.0]; scale to a believable [0, 25%] bonus chance.
        float chance = Math.max(0.0F, Math.min(sappiness * 0.25F, 0.25F));
        if (rand.nextFloat() >= chance) return ItemStack.EMPTY;
        ITreeRoot root = treeRoot();
        return root == null ? ItemStack.EMPTY : root.getMemberStack(tree, EnumGermlingType.SAPLING);
    }

    /** Ticks per operation, scaled by maturation allele speed. */
    public static int computeTicksPerOperation(ITree tree) {
        if (tree == null) return BASE_TICKS_PER_OPERATION;
        float maturation = readFloatAllele(tree.getGenome(), EnumTreeChromosome.MATURATION, 1.0F);
        // Maturation is roughly [0.5, 4.0]; faster maturation = fewer ticks.
        float speed = Math.max(0.25F, maturation);
        int ticks = Math.round(BASE_TICKS_PER_OPERATION / speed);
        return Math.max(MIN_TICKS_PER_OPERATION, ticks);
    }

    private static ItemStack readWood(IAlleleTreeSpecies species) {
        try {
            IWoodProvider provider = species.getWoodProvider();
            if (provider == null) return ItemStack.EMPTY;
            // Try, in order: getWoodStack(), getLogStack(int).
            ItemStack stack = invokeNoArgItemStack(provider, "getWoodStack");
            if (!stack.isEmpty()) return stack;
            stack = invokeIntItemStack(provider, "getLogStack", 1);
            if (!stack.isEmpty()) return stack;
        } catch (Throwable t) {
            ArboricultureMill.LOGGER.debug("Wood lookup failed", t);
        }
        return ItemStack.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private static NonNullList<ItemStack> readFruits(ITree tree, World world, BlockPos pos) {
        try {
            // Standard 5.8.x method: ITree#produceStacks(World, BlockPos, int)
            Method m = tree.getClass().getMethod("produceStacks", World.class, BlockPos.class, int.class);
            Object out = m.invoke(tree, world, pos, Integer.MAX_VALUE / 4);
            if (out instanceof NonNullList) {
                return (NonNullList<ItemStack>) out;
            }
        } catch (NoSuchMethodException nsm) {
            ArboricultureMill.LOGGER.debug("ITree#produceStacks not present in this Forestry build");
        } catch (Throwable t) {
            ArboricultureMill.LOGGER.debug("Fruit lookup failed", t);
        }
        return NonNullList.create();
    }

    private static ItemStack invokeNoArgItemStack(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object result = m.invoke(target);
            if (result instanceof ItemStack) return (ItemStack) result;
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            ArboricultureMill.LOGGER.debug("Reflective call {}# failed", methodName, t);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack invokeIntItemStack(Object target, String methodName, int arg) {
        try {
            Method m = target.getClass().getMethod(methodName, int.class);
            Object result = m.invoke(target, arg);
            if (result instanceof ItemStack) return (ItemStack) result;
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            ArboricultureMill.LOGGER.debug("Reflective call {}#(int) failed", methodName, t);
        }
        return ItemStack.EMPTY;
    }

    private static float readFloatAllele(IGenome genome, EnumTreeChromosome chromosome, float fallback) {
        try {
            IAllele allele = genome.getActiveAllele(chromosome);
            if (allele instanceof IAlleleFloat) {
                return ((IAlleleFloat) allele).getValue();
            }
            if (allele instanceof IAlleleInteger) {
                return ((IAlleleInteger) allele).getValue();
            }
        } catch (Throwable t) {
            ArboricultureMill.LOGGER.debug("Failed to read allele {}", chromosome, t);
        }
        return fallback;
    }
}
