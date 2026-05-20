package com.dbudnik.arboriculturemill.network;

import com.dbudnik.arboriculturemill.tile.TileEntityArboricultureMill;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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

    public static final class Handler implements IMessageHandler<PacketMillSync, IMessage> {
        @Override
        public IMessage onMessage(PacketMillSync message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> applyClientSide(message));
            return null;
        }

        private static void applyClientSide(PacketMillSync m) {
            World world = Minecraft.getMinecraft().world;
            if (world == null) return;
            TileEntity te = world.getTileEntity(m.pos);
            if (te instanceof TileEntityArboricultureMill) {
                ((TileEntityArboricultureMill) te).applyClientSync(
                        m.progress, m.totalTicks, m.energyStored, m.waterStored, m.processing);
            }
        }
    }
}
