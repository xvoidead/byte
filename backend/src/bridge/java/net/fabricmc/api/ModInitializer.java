package net.fabricmc.api;

/** Fabric Loader entrypoint for common code. */
@FunctionalInterface
public interface ModInitializer {
    void onInitialize();
}
