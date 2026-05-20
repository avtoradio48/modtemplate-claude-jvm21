package com.dbudnik.arboriculturemill.capability;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

/**
 * Wraps the internal mill inventory and only exposes the slots that are safe
 * for automation (pipes, hoppers, mod tubes). Saplings and fertilizer can be
 * pushed in, products can be pulled out — but the sapling and fertilizer slots
 * are not extractable, so pipes cannot vacuum them back out.
 */
public final class ExternalItemHandler implements IItemHandlerModifiable {

    private final IItemHandlerModifiable internal;
    private final int[] externalSlots;
    private final boolean[] canInsert;
    private final boolean[] canExtract;

    public ExternalItemHandler(IItemHandlerModifiable internal,
                               int[] externalSlots,
                               boolean[] canInsert,
                               boolean[] canExtract) {
        if (externalSlots.length != canInsert.length || externalSlots.length != canExtract.length) {
            throw new IllegalArgumentException("ExternalItemHandler arrays must match in length");
        }
        this.internal = internal;
        this.externalSlots = externalSlots;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Override
    public int getSlots() {
        return externalSlots.length;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return internal.getStackInSlot(externalSlots[slot]);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (!canInsert[slot]) return stack;
        return internal.insertItem(externalSlots[slot], stack, simulate);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!canExtract[slot]) return ItemStack.EMPTY;
        return internal.extractItem(externalSlots[slot], amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return internal.getSlotLimit(externalSlots[slot]);
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        internal.setStackInSlot(externalSlots[slot], stack);
    }
}
