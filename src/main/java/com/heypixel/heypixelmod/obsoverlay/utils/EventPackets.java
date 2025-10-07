package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.eventbus.api.Event;

public class EventPackets extends Event {
    private Packet<?> packet;
    private EventType type;
    private boolean cancelled;

    public EventType getType() {
        return this.type;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }
    public boolean isCancelled() {
        this.cancelled = true;
        return false;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
    public EventPackets(Packet<?> packet) {
        this.packet = packet;
    }
}