package net.fabricmc.api;

/** Fabric Loader entrypoint for client-only code. */
@FunctionalInterface
public interface ClientModInitializer {
    void onInitializeClient();
}
