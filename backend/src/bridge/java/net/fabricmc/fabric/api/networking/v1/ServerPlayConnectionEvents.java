package net.fabricmc.fabric.api.networking.v1;

import dev.byteide.fabric.TestEvent;
import net.minecraft.network.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

/** The join and disconnect callbacks used in Fabric lessons. */
public final class ServerPlayConnectionEvents {

    public static final TestEvent<Join> JOIN = new TestEvent<>();
    public static final TestEvent<Disconnect> DISCONNECT = new TestEvent<>();

    private ServerPlayConnectionEvents() {
    }

    @FunctionalInterface
    public interface Join {
        void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server);
    }

    @FunctionalInterface
    public interface Disconnect {
        void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server);
    }
}
