package net.minecraft.client;

import net.minecraft.client.network.ClientPlayerEntity;

/** Minimal client stand-in used by the client-side Fabric lesson. */
public final class MinecraftClient {

    public ClientPlayerEntity player = new ClientPlayerEntity("Steve");
}
