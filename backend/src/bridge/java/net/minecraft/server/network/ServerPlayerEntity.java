package net.minecraft.server.network;

import net.minecraft.entity.player.PlayerEntity;

/** Server-side player stand-in used by the Fabric lesson simulator. */
public final class ServerPlayerEntity extends PlayerEntity {
    public ServerPlayerEntity(String name) {
        super(name);
    }
}
