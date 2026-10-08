package net.minecraft.server;

import net.minecraft.world.World;

/** Small in-memory server used by Byte's Fabric lesson simulator. */
public final class MinecraftServer {

    private final PlayerManager playerManager = new PlayerManager();
    private final World overworld = new World();
    private long ticks;

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public World getOverworld() {
        return overworld;
    }

    public long getTicks() {
        return ticks;
    }

    public void advanceTick() {
        ticks++;
    }
}
