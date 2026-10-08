package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import dev.byteide.fabric.TestEvent;
import net.minecraft.client.MinecraftClient;

/** Client tick callback subset used in Byte's Fabric lessons. */
public final class ClientTickEvents {

    public static final TestEvent<EndTick> END_CLIENT_TICK = new TestEvent<>();

    private ClientTickEvents() {
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftClient client);
    }
}
