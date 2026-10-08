package net.minecraft.network;

/** Marker stand-in for Fabric's packet sender in connection callbacks. */
public interface PacketSender {
    PacketSender EMPTY = new PacketSender() {
    };
}
