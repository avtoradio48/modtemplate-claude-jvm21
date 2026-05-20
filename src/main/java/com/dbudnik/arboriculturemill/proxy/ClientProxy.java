package com.dbudnik.arboriculturemill.proxy;

import com.dbudnik.arboriculturemill.network.PacketMillSync;
import com.dbudnik.arboriculturemill.tile.TileEntityArboricultureMill;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Client-side proxy.
 *
 * Item/block model registration is handled by
 * {@link com.dbudnik.arboriculturemill.client.ClientModelRegistration}. The
 * network packet's client-side work also lives here: this class is never
 * loaded on the dedicated server, so the {@code Minecraft} / client-world
 * references below are safe.
 */
public final class ClientProxy extends CommonProxy {

    @Override
    public void handleMillSync(PacketMillSync packet) {
        Minecraft mc = Minecraft.getMinecraft();
        // Network callbacks run off-thread — hop back onto the client thread.
        mc.addScheduledTask(() -> {
            World world = mc.world;
            if (world == null) {
                return;
            }
            TileEntity te = world.getTileEntity(packet.getPos());
            if (te instanceof TileEntityArboricultureMill) {
                ((TileEntityArboricultureMill) te).applyClientSync(
                        packet.getProgress(),
                        packet.getTotalTicks(),
                        packet.getEnergyStored(),
                        packet.getWaterStored(),
                        packet.isProcessing());
            }
        });
    }
}
