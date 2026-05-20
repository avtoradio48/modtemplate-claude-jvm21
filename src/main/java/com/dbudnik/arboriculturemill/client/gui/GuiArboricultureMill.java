package com.dbudnik.arboriculturemill.client.gui;

import com.dbudnik.arboriculturemill.Reference;
import com.dbudnik.arboriculturemill.inventory.ContainerArboricultureMill;
import com.dbudnik.arboriculturemill.tile.TileEntityArboricultureMill;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * IC2-style mill GUI.
 *
 * The static panel — background, slot frames, and the empty energy / water /
 * progress frames — comes from a 256x256 background texture (the GUI itself
 * occupies the top-left 176x184). The dynamic fills (energy gradient, water
 * level, progress arrow) are painted on top with {@link Gui#drawRect}, since
 * they depend on the tile entity's live state.
 */
public class GuiArboricultureMill extends GuiContainer {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(Reference.MOD_ID, "textures/gui/arboriculture_mill.png");

    // Dynamic-fill colours — the frames/backgrounds themselves live in the texture.
    private static final int ENERGY_FILL_TOP = 0xFFC42323;
    private static final int ENERGY_FILL_BOT = 0xFFFFD800;
    private static final int WATER_FILL      = 0xFF2266DD;
    private static final int PROGRESS_FILL   = 0xFFFFFFFF;
    private static final int PROGRESS_FILL_2 = 0xFFCCCCCC;

    // Gauge geometry — must match the frames baked into the texture.
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
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        // The texture is 256x256 with the panel in the top-left corner, so
        // drawTexturedModalRect's fixed 1/256 UV scale maps it 1:1.
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        drawEnergyFill();
        drawWaterFill();
        drawProgressFill();
    }

    private void drawEnergyFill() {
        int stored = tile.getEnergyStorage().getEnergyStored();
        int max = tile.getEnergyStorage().getMaxEnergyStored();
        if (max <= 0) return;
        int filled = (int) ((long) ENERGY_HEIGHT * stored / max);
        if (filled <= 0) return;
        int x = guiLeft + ENERGY_X;
        int y = guiTop + ENERGY_Y;
        // Gradient fill, drawn bottom-up: yellow at the base, red at the top.
        for (int i = 0; i < filled; i++) {
            float t = (float) i / Math.max(1, ENERGY_HEIGHT - 1);
            int rowColor = blendColor(ENERGY_FILL_BOT, ENERGY_FILL_TOP, t);
            int rowY = y + ENERGY_HEIGHT - 1 - i;
            Gui.drawRect(x, rowY, x + ENERGY_WIDTH, rowY + 1, rowColor);
        }
    }

    private void drawWaterFill() {
        FluidStack stack = tile.getWaterTank().getFluid();
        int stored = stack == null ? 0 : stack.amount;
        int max = tile.getWaterTank().getCapacity();
        if (max <= 0) return;
        int filled = (int) ((long) WATER_HEIGHT * stored / max);
        if (filled <= 0) return;
        int x = guiLeft + WATER_X;
        int y = guiTop + WATER_Y;
        Gui.drawRect(x, y + WATER_HEIGHT - filled, x + WATER_WIDTH, y + WATER_HEIGHT, WATER_FILL);
    }

    private void drawProgressFill() {
        int total = tile.getTotalTicks();
        int progress = tile.getProgress();
        if (total <= 0 || !tile.isProcessing()) return;
        int filled = (int) ((long) ARROW_WIDTH * (total - progress) / total);
        if (filled <= 0) return;
        int x = guiLeft + ARROW_X;
        int y = guiTop + ARROW_Y;
        Gui.drawRect(x, y + 2, x + filled, y + ARROW_HEIGHT - 2, PROGRESS_FILL);
        int tipX = Math.min(x + filled, x + ARROW_WIDTH - 2);
        Gui.drawRect(tipX, y + 1, tipX + 2, y + ARROW_HEIGHT - 1, PROGRESS_FILL_2);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = net.minecraft.client.resources.I18n.format("tile.arboriculturemill.arboriculture_mill.name");
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
