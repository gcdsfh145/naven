package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.network.protocol.Packet;
import net.minecraftforge.eventbus.api.Event;

public class PacketEvent2 extends Event {
    private final Packet<?> packet;
    private boolean isCancelled = false;

    public PacketEvent2(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void setCancelled(Boolean state) {
        this.isCancelled = state;
    }
}
