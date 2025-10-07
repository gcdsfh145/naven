package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.network.protocol.Packet;

import static com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils.mc;

public class PacketUtil {
    public static boolean noPacket = false;

    public static void sendPacket(Packet<?> packet) {
        noPacket = true;
        mc.player.connection.send(packet);
        noPacket = false;
    }
}
