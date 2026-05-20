package com.dbudnik.arboriculturemill.capability;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import javax.annotation.Nullable;

/**
 * Water tank that pipes can fill but cannot drain. Internal consumption uses
 * {@link #consumeInternal(int)} which bypasses the locked drain API. The tank
 * is also restricted to a single fluid type so that, e.g., a misrouted lava
 * pipe doesn't poison the buffer.
 */
public final class InputFluidTank extends FluidTank {

    private final Fluid allowedFluid;

    public InputFluidTank(int capacity, Fluid allowedFluid) {
        super(capacity);
        this.allowedFluid = allowedFluid;
        setCanDrain(false);
        setCanFill(true);
    }

    @Override
    public boolean canFillFluidType(FluidStack stack) {
        if (allowedFluid == null) return super.canFillFluidType(stack);
        return stack != null && stack.getFluid() == allowedFluid;
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        return null;
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return null;
    }

    /** Internal drain — bypasses the public drain lock. */
    public boolean consumeInternal(int amount) {
        if (amount <= 0) return true;
        if (fluid == null || fluid.amount < amount) return false;
        fluid.amount -= amount;
        if (fluid.amount <= 0) {
            fluid = null;
        }
        onContentsChanged();
        return true;
    }
}
