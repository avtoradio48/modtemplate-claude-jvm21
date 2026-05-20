package com.dbudnik.arboriculturemill.capability;

import net.minecraftforge.energy.EnergyStorage;

/**
 * RF storage that accepts power from cables but never lets anyone drain it.
 * Internal logic still drains via {@link #consume(int)}; only the external
 * IEnergyStorage capability view is locked for extraction.
 */
public final class InputEnergyStorage extends EnergyStorage {

    public InputEnergyStorage(int capacity, int maxReceive) {
        super(capacity, maxReceive, 0);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    /** Internal-only consumption — bypasses the locked extract API. */
    public boolean consume(int amount) {
        if (amount <= 0) return true;
        if (this.energy < amount) return false;
        this.energy -= amount;
        return true;
    }

    public void setEnergy(int value) {
        this.energy = Math.max(0, Math.min(value, getMaxEnergyStored()));
    }
}
