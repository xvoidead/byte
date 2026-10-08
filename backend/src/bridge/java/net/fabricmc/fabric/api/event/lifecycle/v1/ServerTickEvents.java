package net.fabricmc.fabric.api.event.lifecycle.v1;

import dev.byteide.fabric.TestEvent;
import net.minecraft.server.MinecraftServer;

/** The subset of Fabric's server tick events used in lessons. */
public final class ServerTickEvents {

    public static final TestEvent<EndTick> END_SERVER_TICK = new TestEvent<>();

    private ServerTickEvents() {
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftServer server);
    }
}
