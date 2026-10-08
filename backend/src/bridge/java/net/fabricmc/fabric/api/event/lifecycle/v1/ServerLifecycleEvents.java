package net.fabricmc.fabric.api.event.lifecycle.v1;

import dev.byteide.fabric.TestEvent;
import net.minecraft.server.MinecraftServer;

/** The subset of Fabric's server lifecycle events used in lessons. */
public final class ServerLifecycleEvents {

    public static final TestEvent<ServerStarted> SERVER_STARTED = new TestEvent<>();

    private ServerLifecycleEvents() {
    }

    @FunctionalInterface
    public interface ServerStarted {
        void onServerStarted(MinecraftServer server);
    }
}
