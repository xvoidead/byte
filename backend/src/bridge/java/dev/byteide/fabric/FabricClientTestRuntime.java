package dev.byteide.fabric;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/** Deterministic client-tick/key input simulator for Fabric lesson checks; it does not launch Minecraft. */
public final class FabricClientTestRuntime {

    private final MinecraftClient client = new MinecraftClient();
    private boolean loaded;

    public void load(Class<? extends ClientModInitializer> entrypoint) {
        ClientTickEvents.END_CLIENT_TICK.clear();
        KeyBindingHelper.reset();
        try {
            ClientModInitializer mod = entrypoint.getDeclaredConstructor().newInstance();
            mod.onInitializeClient();
            loaded = true;
        } catch (ReflectiveOperationException e) {
            Throwable cause = e instanceof InvocationTargetException invocation ? invocation.getCause() : e;
            throw new IllegalStateException("Не удалось загрузить Fabric client entrypoint", cause);
        }
    }

    public void pressKey(int keyCode) {
        ensureLoaded();
        KeyBindingHelper.pressKey(keyCode);
    }

    public void tick() {
        ensureLoaded();
        ClientTickEvents.END_CLIENT_TICK.invoke(event -> event.onEndTick(client));
    }

    public List<String> drainMessages() {
        ClientPlayerEntity player = client.player;
        return player == null ? List.of() : player.drainMessages().stream()
                .map(message -> "[" + player.getName().getString() + "] " + message)
                .toList();
    }

    private void ensureLoaded() {
        if (!loaded) {
            throw new IllegalStateException("Сначала загрузите Fabric client entrypoint");
        }
    }
}
