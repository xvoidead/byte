package net.minecraft.server;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Small in-memory online-player list for Fabric lesson checks. */
public final class PlayerManager {

    private final List<ServerPlayerEntity> players = new ArrayList<>();

    public List<ServerPlayerEntity> getPlayerList() {
        return List.copyOf(players);
    }

    public ServerPlayerEntity getPlayer(String name) {
        return players.stream().filter(player -> player.getName().getString().equals(name)).findFirst().orElse(null);
    }

    public void broadcast(Text message, boolean overlay) {
        players.forEach(player -> player.sendMessage(message, overlay));
    }

    public ServerPlayerEntity add(String name) {
        ServerPlayerEntity player = new ServerPlayerEntity(name);
        players.add(player);
        return player;
    }

    public void remove(ServerPlayerEntity player) {
        players.remove(player);
    }
}
