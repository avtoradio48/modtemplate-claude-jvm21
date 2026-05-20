package com.dbudnik.arboriculturemill.init;

import com.dbudnik.arboriculturemill.Reference;
import com.dbudnik.arboriculturemill.client.gui.GuiArboricultureMill;
import com.dbudnik.arboriculturemill.inventory.ContainerArboricultureMill;
import com.dbudnik.arboriculturemill.tile.TileEntityArboricultureMill;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public final class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != Reference.GUI_ID_MILL) return null;
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (!(te instanceof TileEntityArboricultureMill)) return null;
        return new ContainerArboricultureMill(player.inventory, (TileEntityArboricultureMill) te);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != Reference.GUI_ID_MILL) return null;
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (!(te instanceof TileEntityArboricultureMill)) return null;
        return new GuiArboricultureMill(player.inventory, (TileEntityArboricultureMill) te);
    }
}
