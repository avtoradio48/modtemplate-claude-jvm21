package com.dbudnik.arboriculturemill.tile;

import com.dbudnik.arboriculturemill.capability.ExternalItemHandler;
import com.dbudnik.arboriculturemill.capability.InputEnergyStorage;
import com.dbudnik.arboriculturemill.capability.InputFluidTank;
import com.dbudnik.arboriculturemill.integration.TreeRecipeLookup;
import com.dbudnik.arboriculturemill.network.NetworkHandler;
import com.dbudnik.arboriculturemill.network.PacketMillSync;
import com.dbudnik.arboriculturemill.util.IngredientMatcher;
import forestry.api.arboriculture.ITree;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class TileEntityArboricultureMill extends TileEntity implements ITickable {

    public static final int RF_CAPACITY = 100_000;
    public static final int RF_MAX_RECEIVE = 5_000;
    public static final int RF_PER_TICK = 2_500;

    public static final int WATER_CAPACITY = 16_000;
    public static final int WATER_PER_OPERATION = 1_000;
    public static final int FERTILIZER_PER_OPERATION = 1;

    public static final int SLOT_SAPLING = 0;
    public static final int SLOT_FERTILIZER = 1;
    public static final int SLOT_OUTPUT_START = 2;
    public static final int OUTPUT_SLOT_COUNT = 6;
    public static final int TOTAL_SLOTS = SLOT_OUTPUT_START + OUTPUT_SLOT_COUNT;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == SLOT_SAPLING) return IngredientMatcher.isSapling(stack);
            if (slot == SLOT_FERTILIZER) return IngredientMatcher.isFertilizer(stack);
            // Output slots: the IItemHandler must accept inserts so the mill
            // can write products into them. External pipe access is filtered
            // separately by ExternalItemHandler / OutputOnlySlot.
            return true;
        }
    };

    private final IItemHandlerModifiable externalInventory = buildExternalHandler();

    private final InputEnergyStorage energy = new InputEnergyStorage(RF_CAPACITY, RF_MAX_RECEIVE);
    private final InputFluidTank water = new InputFluidTank(WATER_CAPACITY, IngredientMatcher.waterFluid());

    private int progress;
    private int totalTicks;
    private boolean processing;
    private int lastSyncedProgress = -1;
    private int lastSyncedTotal = -1;
    private int lastSyncedEnergy = -1;
    private int lastSyncedWater = -1;

    private IItemHandlerModifiable buildExternalHandler() {
        int[] slots = new int[TOTAL_SLOTS];
        boolean[] insert = new boolean[TOTAL_SLOTS];
        boolean[] extract = new boolean[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slots[i] = i;
            if (i == SLOT_SAPLING || i == SLOT_FERTILIZER) {
                insert[i] = true;
                extract[i] = false;
            } else {
                insert[i] = false;
                extract[i] = true;
            }
        }
        return new ExternalItemHandler(inventory, slots, insert, extract);
    }

    public ItemStackHandler getInternalInventory() {
        return inventory;
    }

    public InputEnergyStorage getEnergyStorage() {
        return energy;
    }

    public InputFluidTank getWaterTank() {
        return water;
    }

    public int getProgress() {
        return progress;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public boolean isProcessing() {
        return processing;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        if (!processing) {
            tryStartOperation();
        } else {
            tickOperation();
        }
        maybeSyncToClients();
    }

    /**
     * Refuses to begin an operation unless every input is satisfied:
     * sapling present, fertilizer present, enough water and energy, and
     * room in the output slots for the projected products. This is the fix
     * for the "starts and waits forever on the last tick" behaviour.
     */
    private void tryStartOperation() {
        ItemStack saplingStack = inventory.getStackInSlot(SLOT_SAPLING);
        if (saplingStack.isEmpty()) return;

        ITree tree = TreeRecipeLookup.asTree(saplingStack);
        if (tree == null) return;

        ItemStack fertilizer = inventory.getStackInSlot(SLOT_FERTILIZER);
        if (fertilizer.getCount() < FERTILIZER_PER_OPERATION
                || !IngredientMatcher.isFertilizer(fertilizer)) return;

        FluidStack tank = water.getFluid();
        if (tank == null || tank.amount < WATER_PER_OPERATION) return;

        // Energy: require at least one tick's worth before we even consider
        // starting. The operation itself can pause mid-way if energy drops.
        if (energy.getEnergyStored() < RF_PER_TICK) return;

        Random random = world.rand;
        NonNullList<ItemStack> projected = TreeRecipeLookup.rollProducts(tree, world, getPos(), random);
        ItemStack bonusSapling = TreeRecipeLookup.rollSaplingBonus(tree, random);
        if (!bonusSapling.isEmpty()) projected.add(bonusSapling);

        if (!hasRoomFor(projected)) return;

        // All conditions met — consume inputs and commit to the operation.
        inventory.extractItem(SLOT_SAPLING, 1, false);
        inventory.extractItem(SLOT_FERTILIZER, FERTILIZER_PER_OPERATION, false);
        water.consumeInternal(WATER_PER_OPERATION);

        totalTicks = TreeRecipeLookup.computeTicksPerOperation(tree);
        progress = totalTicks;
        processing = true;

        // Cache the rolled outputs on the TE so we don't reroll them every tick.
        pendingProducts = projected;
        markDirty();
    }

    /** Rolled products waiting to be emitted when progress reaches zero. */
    private NonNullList<ItemStack> pendingProducts = NonNullList.create();

    private void tickOperation() {
        if (!energy.consume(RF_PER_TICK)) {
            // Pause: keep progress where it is until power returns.
            return;
        }
        progress--;
        if (progress <= 0) {
            finishOperation();
        }
    }

    private void finishOperation() {
        for (ItemStack out : pendingProducts) {
            insertOrDrop(out);
        }
        pendingProducts.clear();
        processing = false;
        progress = 0;
        totalTicks = 0;
        markDirty();
    }

    private boolean hasRoomFor(NonNullList<ItemStack> stacks) {
        // Simulate inserts into the output slots only.
        int[] virtualCounts = new int[OUTPUT_SLOT_COUNT];
        ItemStack[] virtualStacks = new ItemStack[OUTPUT_SLOT_COUNT];
        for (int i = 0; i < OUTPUT_SLOT_COUNT; i++) {
            ItemStack existing = inventory.getStackInSlot(SLOT_OUTPUT_START + i);
            virtualStacks[i] = existing;
            virtualCounts[i] = existing.getCount();
        }
        outer:
        for (ItemStack input : stacks) {
            int remaining = input.getCount();
            for (int i = 0; i < OUTPUT_SLOT_COUNT && remaining > 0; i++) {
                ItemStack existing = virtualStacks[i];
                int max = inventory.getSlotLimit(SLOT_OUTPUT_START + i);
                if (existing.isEmpty()) {
                    int placed = Math.min(remaining, max);
                    virtualStacks[i] = input;
                    virtualCounts[i] = placed;
                    remaining -= placed;
                } else if (ItemStack.areItemsEqual(existing, input)
                        && ItemStack.areItemStackTagsEqual(existing, input)) {
                    int free = max - virtualCounts[i];
                    int placed = Math.min(remaining, free);
                    virtualCounts[i] += placed;
                    remaining -= placed;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private void insertOrDrop(ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (int i = 0; i < OUTPUT_SLOT_COUNT && !remainder.isEmpty(); i++) {
            remainder = inventory.insertItem(SLOT_OUTPUT_START + i, remainder, false);
        }
        if (!remainder.isEmpty() && world != null) {
            BlockPos drop = getPos().up();
            EntityItem entity = new EntityItem(world,
                    drop.getX() + 0.5, drop.getY() + 0.1, drop.getZ() + 0.5,
                    remainder);
            world.spawnEntity(entity);
        }
    }

    private void maybeSyncToClients() {
        if (world == null || world.isRemote) return;
        if (progress != lastSyncedProgress
                || totalTicks != lastSyncedTotal
                || energy.getEnergyStored() != lastSyncedEnergy
                || waterAmount() != lastSyncedWater) {
            lastSyncedProgress = progress;
            lastSyncedTotal = totalTicks;
            lastSyncedEnergy = energy.getEnergyStored();
            lastSyncedWater = waterAmount();
            PacketMillSync packet = new PacketMillSync(getPos(),
                    progress, totalTicks,
                    lastSyncedEnergy, lastSyncedWater, processing);
            NetworkHandler.sendToNearby(packet, this);
        }
    }

    public void applyClientSync(int progress, int totalTicks, int energyStored, int waterStored, boolean processing) {
        this.progress = progress;
        this.totalTicks = totalTicks;
        this.processing = processing;
        this.energy.setEnergy(energyStored);
        int currentAmount = waterAmount();
        if (currentAmount != waterStored) {
            FluidStack syncStack = IngredientMatcher.waterFluid() == null
                    ? null
                    : new FluidStack(IngredientMatcher.waterFluid(), waterStored);
            water.setFluid(syncStack);
        }
    }

    private int waterAmount() {
        FluidStack stack = water.getFluid();
        return stack == null ? 0 : stack.amount;
    }

    public void dropInventoryContents(World world, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            EntityItem entity = new EntityItem(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    stack);
            world.spawnEntity(entity);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Inventory", inventory.serializeNBT());
        NBTTagCompound energyTag = new NBTTagCompound();
        energyTag.setInteger("Stored", energy.getEnergyStored());
        tag.setTag("Energy", energyTag);
        NBTTagCompound waterTag = new NBTTagCompound();
        water.writeToNBT(waterTag);
        tag.setTag("Water", waterTag);
        tag.setInteger("Progress", progress);
        tag.setInteger("TotalTicks", totalTicks);
        tag.setBoolean("Processing", processing);

        net.minecraft.nbt.NBTTagList pending = new net.minecraft.nbt.NBTTagList();
        for (ItemStack out : pendingProducts) {
            NBTTagCompound entry = new NBTTagCompound();
            out.writeToNBT(entry);
            pending.appendTag(entry);
        }
        tag.setTag("Pending", pending);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("Inventory")) {
            inventory.deserializeNBT(tag.getCompoundTag("Inventory"));
        }
        if (tag.hasKey("Energy")) {
            energy.setEnergy(tag.getCompoundTag("Energy").getInteger("Stored"));
        }
        if (tag.hasKey("Water")) {
            water.readFromNBT(tag.getCompoundTag("Water"));
        }
        progress = tag.getInteger("Progress");
        totalTicks = tag.getInteger("TotalTicks");
        processing = tag.getBoolean("Processing");

        pendingProducts.clear();
        if (tag.hasKey("Pending")) {
            net.minecraft.nbt.NBTTagList pending = tag.getTagList("Pending", 10);
            for (int i = 0; i < pending.tagCount(); i++) {
                pendingProducts.add(new ItemStack(pending.getCompoundTagAt(i)));
            }
        }
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), 1, getUpdateTag());
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        if (capability == CapabilityEnergy.ENERGY) return true;
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) (facing == null ? inventory : externalInventory);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) energy;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) water;
        }
        return super.getCapability(capability, facing);
    }
}
