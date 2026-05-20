package com.dbudnik.arboriculturemill.proxy;

/**
 * Client-side proxy. Item/block model registration lives in
 * {@link com.dbudnik.arboriculturemill.client.ClientModelRegistration}
 * (subscribed to {@code ModelRegistryEvent}), so the proxy only needs the
 * shared {@link CommonProxy} behaviour.
 */
public final class ClientProxy extends CommonProxy {
}
