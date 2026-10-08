package net.minecraft.server.network;

/** Connection holder matching the Fabric join/disconnect callback shape. */
public final class ServerPlayNetworkHandler {

    public final ServerPlayerEntity player;

    public ServerPlayNetworkHandler(ServerPlayerEntity player) {
        this.player = player;
    }
}
