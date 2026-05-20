package com.dbudnik.arboriculturemill.client.gui;

import com.dbudnik.arboriculturemill.inventory.ContainerArboricultureMill;
import com.dbudnik.arboriculturemill.tile.TileEntityArboricultureMill;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * IC2-style mill GUI. The whole panel is painted programmatically with
 * {@link Gui#drawRect} using the exact same coordinates as the container
 * slots, so the slot frames always line up. (A custom background texture was
 * dropped on purpose: a PNG that is not 256x256 is sampled by
 * {@code drawTexturedModalRect} as if it were, which stretched the panel and
 * made the layout look "shifted".)
 */
public class GuiArboricultureMill extends GuiContainer {

    private static final int PANEL_COLOR        = 0xFFC6C6C6; // IC2 light grey
    private static final int PANEL_SHADOW       = 0xFF555555;
    private static final int PANEL_HIGHLIGHT    = 0xFFFFFFFF;
    private static final int SLOT_BG            = 0xFF8B8B8B;
    private static final int SLOT_SHADOW        = 0xFF373737;
    private static final int ENERGY_FRAME       = 0xFF1F1F1F;
    private static final int ENERGY_BG          = 0xFF202020;
    private static final int ENERGY_FILL_TOP    = 0xFFC42323;
    private static final int ENERGY_FILL_BOT    = 0xFFFFD800;
    private static final int WATER_FILL         = 0xFF2266DD;
    private static final int WATER_BG           = 0xFF1A2A38;
    private static final int PROGRESS_BG        = 0xFF373737;
    private static final int PROGRESS_FILL      = 0xFFFFFFFF;
    private static final int PROGRESS_FILL_2    = 0xFFCCCCCC;

    // The two vertical bars sit side-by-side at the far left, then the input
    // slots, the progress arrow, and the output grid run left-to-right.
    private static final int ENERGY_X      = 8;
    private static final int ENERGY_Y      = 18;
    private static final int ENERGY_WIDTH  = 14;
    private static final int ENERGY_HEIGHT = 70;

    private static final int WATER_X      = 26;
    private static final int WATER_Y      = 18;
    private static final int WATER_WIDTH  = 14;
    private static final int WATER_HEIGHT = 70;

    private static final int ARROW_X      = 84;
    private static final int ARROW_Y      = 40;
    private static final int ARROW_WIDTH  = 18;
    private static final int ARROW_HEIGHT = 12;

    private final TileEntityArboricultureMill tile;

    public GuiArboricultureMill(InventoryPlayer playerInv, TileEntityArboricultureMill tile) {
        super(new ContainerArboricultureMill(playerInv, tile));
        this.tile = tile;
        this.xSize = ContainerArboricultureMill.GUI_WIDTH;
        this.ySize = ContainerArboricultureMill.GUI_HEIGHT;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawProgrammatic();
        drawEnergyBar();
        drawWaterBar();
        drawProgressArrow();
    }

    private void drawProgrammatic() {
        // Outer panel with bevel.
        Gui.drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, PANEL_COLOR);
        // Top + left highlight
        Gui.drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 1, PANEL_HIGHLIGHT);
        Gui.drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + ySize, PANEL_HIGHLIGHT);
        // Bottom + right shadow
        Gui.drawRect(guiLeft, guiTop + ySize - 1, guiLeft + xSize, guiTop + ySize, PANEL_SHADOW);
        Gui.drawRect(guiLeft + xSize - 1, guiTop, guiLeft + xSize, guiTop + ySize, PANEL_SHADOW);

        // Slot backgrounds.
        drawSlotBackground(ContainerArboricultureMill.SAPLING_SLOT_X, ContainerArboricultureMill.SAPLING_SLOT_Y);
        drawSlotBackground(ContainerArboricultureMill.FERTILIZER_SLOT_X, ContainerArboricultureMill.FERTILIZER_SLOT_Y);
        for (int row = 0; row < ContainerArboricultureMill.OUTPUT_ROWS; row++) {
            for (int col = 0; col < ContainerArboricultureMill.OUTPUT_COLS; col++) {
                drawSlotBackground(
                        ContainerArboricultureMill.OUTPUT_GRID_X + col * 18,
                        ContainerArboricultureMill.OUTPUT_GRID_Y + row * 18);
            }
        }
        // Player inventory.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBackground(
                        ContainerArboricultureMill.PLAYER_INV_X + col * 18,
                        ContainerArboricultureMill.PLAYER_INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBackground(ContainerArboricultureMill.PLAYER_INV_X + col * 18,
                    ContainerArboricultureMill.HOTBAR_Y);
        }
    }

    private void drawSlotBackground(int x, int y) {
        int sx = guiLeft + x - 1;
        int sy = guiTop + y - 1;
        Gui.drawRect(sx, sy, sx + 18, sy + 18, SLOT_SHADOW);
        Gui.drawRect(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BG);
    }

    private void drawEnergyBar() {
        int x = guiLeft + ENERGY_X;
        int y = guiTop + ENERGY_Y;
        Gui.drawRect(x - 1, y - 1, x + ENERGY_WIDTH + 1, y + ENERGY_HEIGHT + 1, ENERGY_FRAME);
        Gui.drawRect(x, y, x + ENERGY_WIDTH, y + ENERGY_HEIGHT, ENERGY_BG);

        int stored = tile.getEnergyStorage().getEnergyStored();
        int max = tile.getEnergyStorage().getMaxEnergyStored();
        if (max <= 0) return;
        int filled = (int) ((long) ENERGY_HEIGHT * stored / max);
        if (filled <= 0) return;
        // Gradient fill, top = red, bottom = yellow (drawn bottom-up).
        for (int i = 0; i < filled; i++) {
            float t = (float) i / Math.max(1, ENERGY_HEIGHT - 1);
            int rowColor = blendColor(ENERGY_FILL_BOT, ENERGY_FILL_TOP, t);
            int rowY = y + ENERGY_HEIGHT - 1 - i;
            Gui.drawRect(x, rowY, x + ENERGY_WIDTH, rowY + 1, rowColor);
        }
    }

    private void drawWaterBar() {
        int x = guiLeft + WATER_X;
        int y = guiTop + WATER_Y;
        Gui.drawRect(x - 1, y - 1, x + WATER_WIDTH + 1, y + WATER_HEIGHT + 1, ENERGY_FRAME);
        Gui.drawRect(x, y, x + WATER_WIDTH, y + WATER_HEIGHT, WATER_BG);

        FluidStack stack = tile.getWaterTank().getFluid();
        int stored = stack == null ? 0 : stack.amount;
        int max = tile.getWaterTank().getCapacity();
        if (max <= 0) return;
        int filled = (int) ((long) WATER_HEIGHT * stored / max);
        if (filled <= 0) return;
        Gui.drawRect(x, y + WATER_HEIGHT - filled, x + WATER_WIDTH, y + WATER_HEIGHT, WATER_FILL);
    }

    private void drawProgressArrow() {
        int x = guiLeft + ARROW_X;
        int y = guiTop + ARROW_Y;

        // Frame.
        Gui.drawRect(x - 1, y - 1, x + ARROW_WIDTH + 1, y + ARROW_HEIGHT + 1, ENERGY_FRAME);
        Gui.drawRect(x, y, x + ARROW_WIDTH, y + ARROW_HEIGHT, PROGRESS_BG);

        int total = tile.getTotalTicks();
        int progress = tile.getProgress();
        if (total <= 0 || !tile.isProcessing()) return;
        int filled = (int) ((long) ARROW_WIDTH * (total - progress) / total);
        if (filled <= 0) return;
        // Body
        Gui.drawRect(x, y + 2, x + filled, y + ARROW_HEIGHT - 2, PROGRESS_FILL);
        // Tip — small triangle hint
        int tipX = Math.min(x + filled, x + ARROW_WIDTH - 2);
        Gui.drawRect(tipX, y + 1, tipX + 2, y + ARROW_HEIGHT - 1, PROGRESS_FILL_2);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = net.minecraft.client.resources.I18n.format("block.arboriculturemill.arboriculture_mill.name");
        fontRenderer.drawString(title, 8, 6, 0x404040);
        String invLabel = net.minecraft.client.resources.I18n.format("container.inventory");
        fontRenderer.drawString(invLabel, ContainerArboricultureMill.PLAYER_INV_X,
                ContainerArboricultureMill.PLAYER_INV_Y - 11, 0x404040);

        renderTooltips(mouseX, mouseY);
    }

    private void renderTooltips(int mouseX, int mouseY) {
        int localX = mouseX - guiLeft;
        int localY = mouseY - guiTop;
        if (inBox(localX, localY, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT)) {
            List<String> lines = new ArrayList<>();
            lines.add(TextFormatting.YELLOW + "Energy");
            lines.add(tile.getEnergyStorage().getEnergyStored() + " / "
                    + tile.getEnergyStorage().getMaxEnergyStored() + " RF");
            lines.add(TextFormatting.GRAY + "Consumes "
                    + TileEntityArboricultureMill.RF_PER_TICK + " RF/t");
            drawHoveringText(lines, mouseX - guiLeft, mouseY - guiTop);
        } else if (inBox(localX, localY, WATER_X, WATER_Y, WATER_WIDTH, WATER_HEIGHT)) {
            List<String> lines = new ArrayList<>();
            FluidStack stack = tile.getWaterTank().getFluid();
            int amt = stack == null ? 0 : stack.amount;
            lines.add(TextFormatting.AQUA + "Water");
            lines.add(amt + " / " + tile.getWaterTank().getCapacity() + " mB");
            lines.add(TextFormatting.GRAY + "Consumes "
                    + TileEntityArboricultureMill.WATER_PER_OPERATION + " mB / operation");
            drawHoveringText(lines, mouseX - guiLeft, mouseY - guiTop);
        } else if (inBox(localX, localY, ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
            List<String> lines = new ArrayList<>();
            lines.add(TextFormatting.WHITE + "Progress");
            if (tile.isProcessing()) {
                lines.add((tile.getTotalTicks() - tile.getProgress()) + " / "
                        + tile.getTotalTicks() + " ticks");
            } else {
                lines.add(TextFormatting.GRAY + "Idle");
            }
            drawHoveringText(lines, mouseX - guiLeft, mouseY - guiTop);
        }
    }

    private static boolean inBox(int x, int y, int bx, int by, int bw, int bh) {
        return x >= bx && x < bx + bw && y >= by && y < by + bh;
    }

    private static int blendColor(int a, int b, float t) {
        t = Math.max(0F, Math.min(1F, t));
        int aA = (a >> 24) & 0xFF, bA = (b >> 24) & 0xFF;
        int aR = (a >> 16) & 0xFF, bR = (b >> 16) & 0xFF;
        int aG = (a >> 8) & 0xFF,  bG = (b >> 8) & 0xFF;
        int aB = a & 0xFF,          bB = b & 0xFF;
        int oA = (int) (aA + (bA - aA) * t);
        int oR = (int) (aR + (bR - aR) * t);
        int oG = (int) (aG + (bG - aG) * t);
        int oB = (int) (aB + (bB - aB) * t);
        return (oA << 24) | (oR << 16) | (oG << 8) | oB;
    }

}
