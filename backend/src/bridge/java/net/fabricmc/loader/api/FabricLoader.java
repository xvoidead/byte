package net.fabricmc.loader.api;

import java.nio.file.Path;

/** Fabric Loader subset; the lesson simulator maps the game config directory to the run folder. */
public final class FabricLoader {

    private static final FabricLoader INSTANCE = new FabricLoader();

    private FabricLoader() {
    }

    public static FabricLoader getInstance() {
        return INSTANCE;
    }

    public Path getConfigDir() {
        return Path.of("config");
    }
}
