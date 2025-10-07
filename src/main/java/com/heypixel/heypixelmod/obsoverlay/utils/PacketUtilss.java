package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

public class PacketUtilss {
    public static boolean noPacket = false;
    private static final Minecraft mc = Minecraft.getInstance();

    public static void sendPacket(Packet<?> packet) {
        noPacket = true;
        mc.player.connection.send(packet);
        noPacket = false;
    }
}
