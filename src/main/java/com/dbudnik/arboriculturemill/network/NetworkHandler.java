package com.dbudnik.arboriculturemill.network;

import com.dbudnik.arboriculturemill.Reference;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {

    private static SimpleNetworkWrapper channel;

    private NetworkHandler() {}

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);
        channel.registerMessage(PacketMillSync.Handler.class, PacketMillSync.class, 0, Side.CLIENT);
    }

    /** Sends a packet to every player tracking the chunk containing {@code te}. */
    public static void sendToNearby(PacketMillSync packet, TileEntity te) {
        if (channel == null || te.getWorld() == null) return;
        int dim = te.getWorld().provider.getDimension();
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(
                dim,
                te.getPos().getX() + 0.5,
                te.getPos().getY() + 0.5,
                te.getPos().getZ() + 0.5,
                64);
        channel.sendToAllAround(packet, point);
    }
}
