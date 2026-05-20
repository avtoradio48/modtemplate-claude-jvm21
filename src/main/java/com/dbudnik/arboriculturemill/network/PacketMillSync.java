package com.dbudnik.arboriculturemill.network;

import com.dbudnik.arboriculturemill.ArboricultureMill;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server -&gt; client sync of the mill's live state (progress, energy, water).
 *
 * IMPORTANT: {@link Handler} must stay free of client-only classes.
 * {@code SimpleNetworkWrapper#registerMessage} instantiates the handler class
 * on BOTH sides, so the dedicated server loads and verifies it. A direct
 * reference to {@code Minecraft} / {@code WorldClient} here crashes the server
 * (the bytecode verifier tries to load a client class for the SERVER side).
 * The client work is therefore delegated to the sided proxy.
 */
public final class PacketMillSync implements IMessage {

    private BlockPos pos;
    private int progress;
    private int totalTicks;
    private int energyStored;
    private int waterStored;
    private boolean processing;

    public PacketMillSync() {}

    public PacketMillSync(BlockPos pos, int progress, int totalTicks,
                          int energyStored, int waterStored, boolean processing) {
        this.pos = pos;
        this.progress = progress;
        this.totalTicks = totalTicks;
        this.energyStored = energyStored;
        this.waterStored = waterStored;
        this.processing = processing;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getProgress() {
        return progress;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getEnergyStored() {
        return energyStored;
    }

    public int getWaterStored() {
        return waterStored;
    }

    public boolean isProcessing() {
        return processing;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        progress = buf.readInt();
        totalTicks = buf.readInt();
        energyStored = buf.readInt();
        waterStored = buf.readInt();
        processing = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(progress);
        buf.writeInt(totalTicks);
        buf.writeInt(energyStored);
        buf.writeInt(waterStored);
        buf.writeBoolean(processing);
    }

    /**
     * Received on the client. Deliberately contains no client-only types —
     * the actual {@code Minecraft}/world work runs in
     * {@code ClientProxy#handleMillSync}.
     */
    public static final class Handler implements IMessageHandler<PacketMillSync, IMessage> {
        @Override
        public IMessage onMessage(PacketMillSync message, MessageContext ctx) {
            ArboricultureMill.proxy.handleMillSync(message);
            return null;
        }
    }
}
