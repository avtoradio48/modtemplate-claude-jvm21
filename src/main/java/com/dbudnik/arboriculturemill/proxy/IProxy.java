package com.dbudnik.arboriculturemill.proxy;

import com.dbudnik.arboriculturemill.network.PacketMillSync;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public interface IProxy {
    default void preInit(FMLPreInitializationEvent event) {}
    default void init(FMLInitializationEvent event) {}
    default void postInit(FMLPostInitializationEvent event) {}

    /**
     * Applies a server -&gt; client mill state sync. No-op on the server
     * (CommonProxy keeps the default); ClientProxy does the real work.
     */
    default void handleMillSync(PacketMillSync packet) {}
}
