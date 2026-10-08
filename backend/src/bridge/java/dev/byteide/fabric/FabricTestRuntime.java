package dev.byteide.fabric;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Deterministic, deliberately small Fabric runtime for lesson checks. It does not boot Minecraft;
 * it dispatches the lifecycle, player, tick and block callbacks exercised by the lessons.
 */
public final class FabricTestRuntime {

    private final MinecraftServer server = new MinecraftServer();
    private final List<ServerPlayerEntity> knownPlayers = new ArrayList<>();
    private boolean loaded;

    public void load(Class<? extends ModInitializer> entrypoint) {
        clearEvents();
        try {
            ModInitializer mod = entrypoint.getDeclaredConstructor().newInstance();
            mod.onInitialize();
            loaded = true;
            ServerLifecycleEvents.SERVER_STARTED.invoke(event -> event.onServerStarted(server));
        } catch (ReflectiveOperationException e) {
            Throwable cause = e instanceof InvocationTargetException invocation ? invocation.getCause() : e;
            throw new IllegalStateException("Не удалось загрузить Fabric entrypoint", cause);
        }
    }

    public ServerPlayerEntity join(String name) {
        ensureLoaded();
        ServerPlayerEntity player = server.getPlayerManager().add(name);
        knownPlayers.add(player);
        ServerPlayNetworkHandler handler = new ServerPlayNetworkHandler(player);
        ServerPlayConnectionEvents.JOIN.invoke(event -> event.onPlayReady(handler, PacketSender.EMPTY, server));
        return player;
    }

    public void quit(String name) {
        ensureLoaded();
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
        if (player == null) {
            return;
        }
        ServerPlayNetworkHandler handler = new ServerPlayNetworkHandler(player);
        ServerPlayConnectionEvents.DISCONNECT.invoke(event -> event.onPlayDisconnect(handler, server));
        server.getPlayerManager().remove(player);
    }

    public void tick() {
        ensureLoaded();
        server.advanceTick();
        ServerTickEvents.END_SERVER_TICK.invoke(event -> event.onEndTick(server));
    }

    public boolean breakBlock(String playerName, int x, int y, int z, String blockName) {
        ensureLoaded();
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        if (player == null) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = new BlockState(blockName);
        boolean[] allowed = {true};
        PlayerBlockBreakEvents.BEFORE.invoke(event -> {
            if (!event.before(server.getOverworld(), player, pos, state, (BlockEntity) null)) {
                allowed[0] = false;
            }
        });
        if (allowed[0]) {
            PlayerBlockBreakEvents.AFTER.invoke(event ->
                    event.after(server.getOverworld(), player, pos, state, (BlockEntity) null));
        }
        return allowed[0];
    }

    public List<String> drainMessages() {
        List<String> output = new ArrayList<>();
        for (ServerPlayerEntity player : knownPlayers) {
            String message;
            while ((message = player.nextMessage()) != null) {
                output.add("[" + player.getName().getString() + "] " + message);
            }
        }
        return List.copyOf(output);
    }

    public MinecraftServer server() {
        return server;
    }

    private void ensureLoaded() {
        if (!loaded) {
            throw new IllegalStateException("Сначала загрузите Fabric entrypoint");
        }
    }

    private static void clearEvents() {
        ServerLifecycleEvents.SERVER_STARTED.clear();
        ServerTickEvents.END_SERVER_TICK.clear();
        PlayerBlockBreakEvents.BEFORE.clear();
        PlayerBlockBreakEvents.AFTER.clear();
        ServerPlayConnectionEvents.JOIN.clear();
        ServerPlayConnectionEvents.DISCONNECT.clear();
    }
}
