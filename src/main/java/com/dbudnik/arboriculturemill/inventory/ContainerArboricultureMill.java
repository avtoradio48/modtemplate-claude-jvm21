package com.dbudnik.arboriculturemill.inventory;

import com.dbudnik.arboriculturemill.tile.TileEntityArboricultureMill;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerArboricultureMill extends Container {

    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 184;

    public static final int SAPLING_SLOT_X = 38;
    public static final int SAPLING_SLOT_Y = 24;

    public static final int FERTILIZER_SLOT_X = 38;
    public static final int FERTILIZER_SLOT_Y = 54;

    public static final int OUTPUT_GRID_X = 116;
    public static final int OUTPUT_GRID_Y = 18;
    public static final int OUTPUT_COLS = 3;
    public static final int OUTPUT_ROWS = 2;

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 102;
    public static final int HOTBAR_Y = 160;

    private final TileEntityArboricultureMill tile;

    public ContainerArboricultureMill(InventoryPlayer playerInv, TileEntityArboricultureMill tile) {
        this.tile = tile;

        // Sapling slot — accepts only valid saplings (filter on tile inventory).
        addSlotToContainer(new SlotItemHandler(tile.getInternalInventory(),
                TileEntityArboricultureMill.SLOT_SAPLING,
                SAPLING_SLOT_X, SAPLING_SLOT_Y));

        // Fertilizer slot.
        addSlotToContainer(new SlotItemHandler(tile.getInternalInventory(),
                TileEntityArboricultureMill.SLOT_FERTILIZER,
                FERTILIZER_SLOT_X, FERTILIZER_SLOT_Y));

        // 6 output slots, 3x2 grid.
        for (int row = 0; row < OUTPUT_ROWS; row++) {
            for (int col = 0; col < OUTPUT_COLS; col++) {
                int index = TileEntityArboricultureMill.SLOT_OUTPUT_START + row * OUTPUT_COLS + col;
                addSlotToContainer(new OutputOnlySlot(tile.getInternalInventory(), index,
                        OUTPUT_GRID_X + col * 18,
                        OUTPUT_GRID_Y + row * 18));
            }
        }

        // Player main inventory.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, 9 + row * 9 + col,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }

        // Player hotbar.
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    public TileEntityArboricultureMill getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        if (tile.isInvalid()) return false;
        return player.getDistanceSqToCenter(tile.getPos()) <= 64.0;
    }

    private static final int TILE_SLOT_COUNT = TileEntityArboricultureMill.TOTAL_SLOTS;

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return result;

        ItemStack stack = slot.getStack();
        result = stack.copy();

        if (index < TILE_SLOT_COUNT) {
            // From machine → player inventory.
            if (!mergeItemStack(stack, TILE_SLOT_COUNT, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // From player → machine. Try sapling slot, then fertilizer.
            if (tile.getInternalInventory().isItemValid(TileEntityArboricultureMill.SLOT_SAPLING, stack)
                    && mergeItemStack(stack, 0, 1, false)) {
                // merged
            } else if (tile.getInternalInventory().isItemValid(TileEntityArboricultureMill.SLOT_FERTILIZER, stack)
                    && mergeItemStack(stack, 1, 2, false)) {
                // merged
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();

        return result;
    }

    /** Output-only slot: nothing can be placed by the player, only taken. */
    private static final class OutputOnlySlot extends SlotItemHandler {
        OutputOnlySlot(net.minecraftforge.items.IItemHandler inv, int slot, int x, int y) {
            super(inv, slot, x, y);
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            return false;
        }
    }
}
